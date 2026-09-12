package com.example.hydracontrol.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface HydraApiService {
    @POST("ubus")
    suspend fun callUbus(@Body request: UbusRequest): Response<UbusResponse<Any>>

    @FormUrlEncoded
    @POST("cgi-bin/luci/admin/services/hydraroute/add_domain")
    suspend fun addDomain(
        @Header("Cookie") cookie: String,
        @Field("domain") domain: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("cgi-bin/luci/admin/services/hydraroute/set_outbound")
    suspend fun switchVpnOutbound(
        @Header("Cookie") cookie: String,
        @Field("server_id") serverId: String
    ): Response<ResponseBody>
}
