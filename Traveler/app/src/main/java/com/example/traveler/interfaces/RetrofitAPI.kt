package com.example.traveler.interfaces

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH

interface RetrofitAPI {
    @GET("/todos") // 서버에 GET 요청을 할 주소 입력
    fun getTodoList() : Call<JsonObject> // json 파일을 가져오는 메소드
}