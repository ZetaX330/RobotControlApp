package com.example.rcapp.data.repository

import android.util.Log
import com.example.rcapp.ServerConfig
import com.example.rcapp.data.network.RetrofitClient
import com.example.rcapp.data.network.RetrofitClient.apiService
import com.example.rcapp.model.User

import okhttp3.OkHttpClient

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.io.IOException

class UserRepository {
    companion object {

        fun login(phone: String, password: String, callback: (Boolean, String) -> Unit) {
            val requestBody = mapOf("phone" to phone, "password" to password)
            val call = apiService.login(requestBody)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        callback(true, "登陆成功")
                    } else {
                        callback(false, "Login failed: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    callback(false, "Network error: ${t.message}")
                }
            })
        }

        /**
         * 实现apiService接口
         * enqueue 方法用于将网络请求加入队列，并在后台线程中异步执行，不会阻塞主线程
         */
        fun register(user: User, callback: (Boolean, String) -> Unit) {
            val call = apiService.register(user)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        callback(true, "注册成功")
                    } else {
                        // 处理错误响应
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // 处理请求失败
                }
            })
        }
    }
}