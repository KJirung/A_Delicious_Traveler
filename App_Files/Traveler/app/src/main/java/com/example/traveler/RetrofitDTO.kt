package com.example.traveler

class RetrofitDTO {
    data class TodoInfo1(val todo1: TodoInfo)
    data class TodoInfo2(val todo2: TodoInfo)

    data class TodoInfo(val task: String)
}