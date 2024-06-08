package com.example.traveler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveler.databinding.ActivityResultBinding
import com.example.traveler.interfaces.RetrofitAPI
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    lateinit var mRetrofit : Retrofit // 사용할 레트로핏 객체
    lateinit var mRetrofitAPI: RetrofitAPI // 레트로핏 api 객체
    lateinit var mCallTodoList : retrofit2.Call<JsonObject> // Json 형식의 데이터를 요청하는 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var labels = application.assets.open("classification_label_En.txt").bufferedReader().readLines()
        var labels2 = application.assets.open("classification_label_En2.txt").bufferedReader().readLines()
        var youtube = application.assets.open("Youtube_key.txt").bufferedReader().readLines()

        val index = intent.getIntExtra("index", 0)

//        println("index: $index")

        // Food Name
        binding.foodName1.text = labels[index]
        binding.foodName2.text = labels2[index]

        // Youtube
        var video = youtube[index]
        binding.webView.getSettings().setJavaScriptEnabled(true)
        binding.webView.loadData(video, "text/html", "utf-8")
        binding.webView.setWebChromeClient(WebChromeClient())

        setRetrofit()
        callTodoList()


        // Chat Bot
        binding.Chatbotbtn.setOnClickListener {
            val intent_chat = Intent(this@ResultActivity, ChatBot::class.java)
            startActivity(intent_chat)
        }
    }

    //retrofit 객체 생성
    private fun setRetrofit(){
        //레트로핏으로 가져올 url설정하고 세팅
        mRetrofit = Retrofit
            .Builder()
            .baseUrl(getString(R.string.baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //인터페이스로 만든 레트로핏 api요청 받는 것 변수로 등록
        //retrofit 객체를 통해 인터페이스 생성
        mRetrofitAPI = mRetrofit.create(RetrofitAPI::class.java)
    }

    private fun callTodoList(){
        mCallTodoList = mRetrofitAPI.getTodoList() // RetrofitAPI 에서 JSON 객체를 요청해서 반환하는 메소드 호출
        mCallTodoList.enqueue(mRetrofitCallback) // 응답을 큐에 넣어 대기 시켜놓음. 즉, 응답이 생기면 뱉어낸다.
    }

    private val mRetrofitCallback = (object : retrofit2.Callback<JsonObject>{
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            val result = response.body()
            Log.d("testt", "결과는 ${result}")

            var gson = Gson()
            val dataParsed1 = gson.fromJson(result, RetrofitDTO.TodoInfo1::class.java)
            val dataParsed2 = gson.fromJson(result, RetrofitDTO.TodoInfo2::class.java)

            binding.infoRecipe.text = "\n=== Information ===\n${dataParsed1.todo1.task}\n\n=== Recipe ===\n${dataParsed2.todo2.task}\n"
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            t.printStackTrace()
            Log.d("testt", "에러입니다. ${t.message}")
            binding.infoRecipe.text = "에러입니다. ${t.message}"
        }
    })
}
