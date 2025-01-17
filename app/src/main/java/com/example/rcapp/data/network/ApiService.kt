package com.example.rcapp.data.network

import com.example.rcapp.ServerConfig
import com.example.rcapp.model.User
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @Body注解表示user对象将被序列化并作为HTTP请求的body发送
 * Call是Retrofit用于封装HTTP请求的类型，ResponseBody表示我们期望从服务器接收原始响应体
 */
interface ApiService {
    @POST(ServerConfig.LOGIN_ENDPOINT)
    fun login(@Body requestBody: Map<String, String>): Call<ResponseBody>
    @POST(ServerConfig.REGISTER_ENDPOINT)
    fun register(@Body user: User): Call<ResponseBody>
}