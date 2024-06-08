package com.example.traveler

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveler.ml.InceptionV3
import com.example.traveler.databinding.ActivityClassificationBinding

import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import kotlin.math.sqrt


class ClassificationActivity : AppCompatActivity() {
    data class WeibullModel(val shape: Float, val loc: Float, val scale: Float)
    private lateinit var binding: ActivityClassificationBinding

    lateinit var bitmap: Bitmap
    lateinit var meanVectors: Array<FloatArray>
    lateinit var weibullParams: Array<WeibullModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra("cropped_image_uri")
        if (imagePath != null) {
            bitmap = BitmapFactory.decodeFile(imagePath)
            binding.imageViewPreview.setImageBitmap(bitmap)  // 이미지 뷰에 로드된 이미지 표시
        } else {
            Toast.makeText(this, "이미지 경로가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }


        loadOpenMaxParams()

        //image processor
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(299, 299, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        binding.imagePred.setOnClickListener{
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            tensorImage = imageProcessor.process(tensorImage)

            val model = InceptionV3.newInstance(this)

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 299, 299, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(tensorImage.buffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            val openMaxScores = computeOpenMax(outputFeature0)

            openMaxScores.indices.forEach { i ->
                val scores = openMaxScores[i]
//                println("openMaxScores[$i]: $scores")
            }

            val maxIdx = openMaxScores.indices.maxByOrNull { openMaxScores[it] } ?: -1
//            println("maxIdx: $maxIdx")
            val others_threshold = 0.40


            if (openMaxScores[maxIdx] < others_threshold) {
                var p = Intent(this@ClassificationActivity, MainActivity::class.java)
                startActivity(p)
                val msg = "It's not Korean food!"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            } else {
                var i = Intent(this@ClassificationActivity, ResultActivity::class.java)
                i.putExtra("index", maxIdx)
                startActivity(i)
            }

            // Releases model resources if no longer used.
            model.close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data:Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 100){
            var url = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, url)
            binding.imageViewPreview.setImageBitmap(bitmap)
        }
    }

    private fun softmax(values: FloatArray): FloatArray {
        val expValues = values.map { Math.exp(it.toDouble()).toFloat() }
        val sumExpValues = expValues.sum()
        return expValues.map { it / sumExpValues }.toFloatArray()
    }

    private fun computeOpenMax(logits: FloatArray, alpha: Float = 0.1f): FloatArray {
        logits.indices.forEach { i ->
            val scores = logits[i]
//            println("logits[$i]: $scores")
        }

        val weibullcdf = FloatArray(logits.size) { 0f } // size: 150

        // 극단 분포 CDF 값 계산
        logits.indices.forEach { i ->
            val dist = sqrt(meanVectors[i].indices.sumOf { idx ->
                val diff = logits[idx].toDouble() - meanVectors[i][idx].toDouble()
                diff * diff
            })

//            println("dist: $dist")

            val shape = weibullParams[i].shape.toDouble()
            val loc = weibullParams[i].loc.toDouble()                        // loc 값을 Double로 변환
            val scale = weibullParams[i].scale.toDouble()                    // scale 값을 Double로 변환

            weibullcdf[i] = 1f - Math.exp(-Math.pow((dist - loc) / scale, shape)).toFloat()
        }

        // weibull_cdf와 logits의 차이를 저장할 리스트 생성
        val mulList = mutableListOf<Float>()

        // weibull_cdf와 logits의 곱을 계산하여 리스트에 저장
        weibullcdf.indices.forEach { i ->
            mulList.add(weibullcdf[i] * logits[i])
        }

        // logits의 각 요소에서 mulList의 각 요소를 뺀 값을 저장할 리스트 생성
        val diffList = FloatArray(logits.size + 1)

        diffList[logits.size] = mulList.sum()

        // logits의 각 요소와 mulList의 각 요소의 차이를 계산하여 리스트에 저장
        for (i in logits.indices) {
            diffList[i] = logits[i] - mulList[i]
        }

        // Softmax 확률 계산
        return softmax(diffList)
    }

    fun loadJSONFromAsset(context: Context, fileName: String): String? {
        val json: String? = try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    fun loadOpenMaxParams() {
        val jsonString = loadJSONFromAsset(this, "openmax_params_train.json")
        val jsonObject = JSONObject(jsonString ?: "")
        val meanVecs = jsonObject.getJSONArray("mean_vectors")
        val weibModels = jsonObject.getJSONArray("weibull_models")

        meanVectors = Array(meanVecs.length()) { i ->
            FloatArray(meanVecs.getJSONArray(i).length()) { j ->
                meanVecs.getJSONArray(i).getDouble(j).toFloat()
            }
        }

//        meanVectors.forEachIndexed { index, vector ->
//            println("meanVectors[$index]: ${vector.contentToString()}")
//        }

        weibullParams = Array(weibModels.length()) { i ->
            val model = weibModels.getJSONObject(i)
            WeibullModel(
                model.getDouble("shape").toFloat(),
                model.getDouble("loc").toFloat(),
                model.getDouble("scale").toFloat()
            )
        }
    }

}