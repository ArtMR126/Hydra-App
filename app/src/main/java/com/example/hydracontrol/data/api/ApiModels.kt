package com.example.hydracontrol.data.api

import com.google.gson.annotations.SerializedName

data class UbusRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "call",
    val params: List<Any>
)

data class UbusResponse<T>(
    val jsonrpc: String,
    val id: Int,
    val result: List<Any>?
)

data class LoginResult(
    @SerializedName("ubus_rpc_session")
    val session: String
)
