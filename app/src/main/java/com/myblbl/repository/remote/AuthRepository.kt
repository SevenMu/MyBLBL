package com.myblbl.repository.remote

import com.myblbl.model.BaseResponse
import com.myblbl.model.user.ScanQrModel
import com.myblbl.model.user.SignInResultModel
import com.myblbl.model.user.SsoListModel
import com.myblbl.network.api.ApiService
import com.myblbl.network.session.NetworkSessionGateway

class AuthRepository(
    private val apiService: ApiService,
) {

    suspend fun getQrCode(): Result<BaseResponse<ScanQrModel>> =
        runCatching {
            apiService.getSignInQrCode()
        }

    suspend fun checkSignInResult(qrcodeKey: String, bRet: String): Result<BaseResponse<SignInResultModel>> =
        runCatching {
            apiService.checkSignInResult(qrcodeKey, bRet = bRet)
        }

    suspend fun getSsoList(csrf: String): Result<BaseResponse<SsoListModel>> =
        runCatching {
            apiService.getSsoList(csrf)
        }

    suspend fun setSso(url: String, bRet: String): Result<BaseResponse<Any>> =
        runCatching {
            apiService.setSso(url, bRet = bRet)
        }
}
