package com.example.traveler

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveler.R.id
import com.example.traveler.R.layout
import com.example.traveler.ml.Yolov5m
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class Box(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
)

class Detect : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var bitmap: Bitmap
    lateinit var model: Yolov5m
    lateinit var labels:List<String>
    var boxes1: MutableList<Box> = mutableListOf()

    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(layout.activity_detect)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getAlbum()

        labels = FileUtil.loadLabels(this, "detect_label.txt")
        model = Yolov5m.newInstance(this)
        imageView = findViewById((id.imageView))
        imageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y

                // ImageView와 실제 이미지 사이의 비율 계산
                val scaleFactorX = bitmap.width.toFloat() / imageView.width.toFloat()
                val scaleFactorY = bitmap.height.toFloat() / imageView.height.toFloat()

                // CENTER_CROP을 고려한 좌표 조정
                val adjustedX = (x + (bitmap.width - imageView.width * scaleFactorX) / 2) * scaleFactorX
                val adjustedY = (y + (bitmap.height - imageView.height * scaleFactorY) / 2) * scaleFactorY

//                println("Adjusted coordinates: $adjustedX, $adjustedY")

                val selectedBox = boxes1.find { box ->
                    adjustedX >= box.left && adjustedX <= box.right && adjustedY >= box.top && adjustedY <= box.bottom
                }
                selectedBox?.let {
                    cropAndDisplayImage(it)
                }
            }
            true
        }
    }

    private fun getAlbum(){
        var intent = Intent()
        intent.setAction(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        //intent를 통해 설정된 activity로 이동을 시켜주는 메소드
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 100) {
            var uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            get_predictions();
        }
    }

    fun get_predictions() {
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)
        val imageBuffer = image.getBuffer()

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 3, 640, 640), DataType.FLOAT32)
        inputFeature0.loadBuffer(imageBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val boxes = decodePredictions(outputFeature0)

        drawBoxes(boxes)
    }

    fun decodePredictions(tensorBuffer: TensorBuffer): List<Box> {
        val items = mutableListOf<Box>()
        val data = tensorBuffer.floatArray
        val numDetections = data.size / 6  // 각 탐지당 6개, (512000,)

        val tmpValues = mutableListOf<Float>()  // 'tmp' 값들을 저장할 리스트 생성

        for (i in 0 until numDetections) {
            val offset = i * 6
            val x = data[offset]
            val y = data[offset + 1]
            val width = data[offset + 2]
            val height = data[offset + 3]
            val score = data[offset + 4]
            val confidence = data[offset + 5]
            val tmp = score * confidence
            tmpValues.add(tmp)
//            println("[$i] x, y, width, height, confidence, : $x, $y, $width, $height, $score, $confidence, $tmp")

            if (tmp > 0.5) {  // 신뢰도 임계값 검사
                val left = x - width / 2
                val top = y - height / 2
                val right = x + width / 2
                val bottom = y + height / 2

//                println("left, top, right, bottom: $left, $top, $right, $bottom")

                items.add(Box(left, top, right, bottom))
            }
        }
        // 'tmpValues' 리스트에서 최대값을 찾습니다.
        val highestTmp = tmpValues.maxOrNull() ?: Float.MIN_VALUE  // 최대값을 찾고, 없다면 최소 float 값을 반환

//        println("Highest tmp value: $highestTmp")  // 가장 큰 'tmp' 값 출력

        return items
    }


    fun drawBoxes(boxes: List<Box>) {
        val scaleFactorX = imageView.width.toFloat() / bitmap.width.toFloat()
        val scaleFactorY = imageView.height.toFloat() / bitmap.height.toFloat()

        // 색상 목록 정의
        val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)

        val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        boxes1.clear()  // 이전 바운딩 박스 데이터 삭제

        boxes1.forEachIndexed { index, box ->
            // 색상 순환 사용
            val paint = Paint().apply {
                color = colors[index % colors.size]
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

            // 조정된 바운딩 박스 좌표 계산
            val leftOriginal = box.left * scaleFactorX
            val topOriginal = box.top * scaleFactorY
            val rightOriginal = box.right * scaleFactorX
            val bottomOriginal = box.bottom * scaleFactorY

            // 새로운 바운딩 박스 데이터 생성
            val newBox = Box(leftOriginal, topOriginal, rightOriginal, bottomOriginal)

            // 원본 이미지에 맞춰 바운딩 박스 그리기
//            canvas.drawRect(leftOriginal, topOriginal, rightOriginal, bottomOriginal, paint)
            canvas.drawRect(box.left, box.top, box.right, box.bottom, paint)
        }

        imageView.setImageBitmap(tempBitmap)
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val contextWrapper = ContextWrapper(applicationContext)
        val directory = contextWrapper.getDir("images", Context.MODE_PRIVATE)
        val file = File(directory, "${UUID.randomUUID()}.jpg")

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        return file
    }

    fun cropAndDisplayImage(box: Box) {
        val cropLeft = (box.left).toInt()
        val cropTop = (box.top).toInt()
        val cropWidth = (box.right - box.left).toInt()
        val cropHeight = (box.bottom - box.top).toInt()

        if (cropWidth > 0 && cropHeight > 0) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)

            // 임시 파일로 비트맵 저장
            val file = saveBitmapToFile(croppedBitmap)

//            imageView.setImageBitmap(croppedBitmap)
            val intent = Intent(this@Detect, ClassificationActivity::class.java).apply {
                putExtra("cropped_image_uri", file.absolutePath)
            }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}