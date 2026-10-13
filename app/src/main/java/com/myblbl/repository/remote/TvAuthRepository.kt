package com.myblbl.repository.remote

import com.myblbl.model.user.TvPollData
import com.myblbl.model.user.TvQrCodeData
import com.myblbl.model.BaseResponse
import com.myblbl.network.api.ApiService
import com.myblbl.network.security.AppSignUtils

class TvAuthRepository(
    private val apiService: ApiService
) {
    suspend fun generateTvQrCode(): BaseResponse<TvQrCodeData> {
        val params = mutableMapOf(
            "appkey" to AppSignUtils.TV_APP_KEY,
            "local_id" to "0",
            "platform" to "android",
            "mobi_app" to "android_hd",
            "ts" to AppSignUtils.getTimestamp().toString()
        )
        val signed = AppSignUtils.signForTvLogin(params)
        return apiService.generateTvQrCode(signed)
    }

    suspend fun pollTvQrCode(authCode: String): BaseResponse<TvPollData> {
        val params = mutableMapOf(
            "appkey" to AppSignUtils.TV_APP_KEY,
            "auth_code" to authCode,
            "local_id" to "0",
            "ts" to AppSignUtils.getTimestamp().toString()
        )
        val signed = AppSignUtils.signForTvLogin(params)
        return apiService.pollTvQrCode(signed)
    }
}
