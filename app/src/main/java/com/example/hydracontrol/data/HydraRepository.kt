package com.example.hydracontrol.data

import com.example.hydracontrol.data.api.HydraApiService
import com.example.hydracontrol.data.api.UbusRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class HydraRepository(private val sessionManager: SessionManager) {

    private fun getClient(): HydraApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(sessionManager.routerHost.removeSuffix("/") + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(HydraApiService::class.java)
    }

    suspend fun login(user: String, pass: String): Boolean {
        return try {
            val req = UbusRequest(
                method = "call",
                params = listOf("00000000000000000000000000000000", "session", "login", mapOf("username" to user, "password" to pass))
            )
            val res = getClient().callUbus(req)
            if (res.isSuccessful && res.body()?.result?.getOrNull(0) == 0) {
                val data = res.body()?.result?.getOrNull(1) as? Map<*, *>
                val token = data?.get("ubus_rpc_session") as? String
                sessionManager.authToken = token
                sessionManager.currentCookie = "sysauth=$token"
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addDomain(domain: String): Boolean {
        val cookie = sessionManager.currentCookie ?: return false
        return try {
            val response = getClient().addDomain(cookie, domain.trim())
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun switchVpnServer(serverId: String): Boolean {
        val cookie = sessionManager.currentCookie ?: return false
        return try {
            val response = getClient().switchVpnOutbound(cookie, serverId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
