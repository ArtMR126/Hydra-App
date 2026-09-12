package com.example.hydracontrol.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "hydra_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var routerHost: String
        get() = prefs.getString("router_host", "http://192.168.1.1") ?: "http://192.168.1.1"
        set(value) = prefs.edit().putString("router_host", value).apply()

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var currentCookie: String?
        get() = prefs.getString("sysauth_cookie", null)
        set(value) = prefs.edit().putString("sysauth_cookie", value).apply()
}
