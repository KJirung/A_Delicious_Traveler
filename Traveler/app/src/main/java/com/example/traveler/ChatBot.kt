package com.example.traveler

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ChatBot : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var welcomeTextView: TextView
    lateinit var messageEditText: EditText
    lateinit var sendButton: ImageButton
    val messageList: MutableList<Message> = mutableListOf()
    val messageAdapter: MessageAdapter = MessageAdapter(messageList)

    val JSON: MediaType = "application/json; charset=utf-8"?.toMediaTypeOrNull() ?: throw IllegalStateException("Failed to parse MediaType")
    val client = OkHttpClient()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_bot)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        recyclerView = findViewById(R.id.recycler_view)
        welcomeTextView = findViewById(R.id.welcome_text)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_btn)

        recyclerView.adapter = messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        recyclerView.layoutManager = llm


        sendButton.setOnClickListener {
            val question = messageEditText.text.toString().trim()
            addToChat(question, Message.SENT_BY_ME);
            messageEditText.setText("")
            callAPI(question)
            welcomeTextView.visibility = View.GONE

        }
    }

    fun addToChat(message: String, sentBy: String) {
        runOnUiThread {
            messageList.add(Message(message, sentBy))  // messageList 를 직접 사용
            messageAdapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    fun addResponse(response: String) {
        messageList.removeLast()
        addToChat(response, Message.SENT_BY_BOT)
    }

    fun callAPI(question: String) {
        messageList.add(Message("Typing...", Message.SENT_BY_BOT))

        var jsonBody: JSONObject?  // Nullable type to indicate potential exception

        try {
            val jsonObject = JSONObject().apply {
                put("model", "gpt-3.5-turbo-instruct")
                put("prompt", question)
                put("max_tokens", 4000)
                put("temperature", 0.0)
            }
            jsonBody = jsonObject
        } catch (e: Exception) {
            // Handle the exception here, e.g., log the error
            e.printStackTrace()
            jsonBody = null  // Set jsonBody to null if there's an exception
        }

//        val body = RequestBody.create(jsonBody.toString(), JSON)
        val body = RequestBody.create(JSON ?: throw IllegalStateException("JSON media type cannot be null"), jsonBody.toString())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/completions")
            .header("Authorization", "Bearer sk-proj-ywgX4RHpk311vc9get4aT3BlbkFJvImsXfBAyX55QIVHy9mh")
            .post(body)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                addResponse("응답을 불러오는데 실패했습니다. 이유: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(response.body?.string() ?: "")
                        val jsonArray = jsonObject.getJSONArray("choices")
                        val result = jsonArray.getJSONObject(0).getString("text").trim()
                        addResponse(result)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    println("**응답 실패:**")
                    println("본문: ${response.body?.string()}")
                    addResponse("응답을 불러오는데 실패했습니다. 이유: ${response.body?.toString() ?: "본문 없음"}")
                }
            }
        })
    }
}