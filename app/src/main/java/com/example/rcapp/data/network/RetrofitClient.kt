package com.example.rcapp.data.network

import android.util.Log
import com.example.rcapp.config.ServerConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    /**
     * lazy 是 Kotlin 的一个特性，表示属性的值只有在第一次访问时才会初始化
     * OkHttp 提供的日志拦截器，用于打印 HTTP 请求和响应的日志。
     * 配置了日志级别为 BODY，表示会打印完整的请求和响应体
     * Builder构建OkHttpClient实例
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // 自定义输出，例如使用 Log.d
            Log.d("HTTP_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ServerConfig.SERVER_URL)
            .client(okHttpClient)
            //数据转换器，用于将服务器返回的 JSON 数据自动解析为 Kotlin 对象
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    //使用 Retrofit 的 create 方法，动态生成 ApiService 接口的实现
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}