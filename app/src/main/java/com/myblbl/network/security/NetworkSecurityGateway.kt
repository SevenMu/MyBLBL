package com.myblbl.network.security

import com.myblbl.network.NetworkManager

interface NetworkSecurityGateway {
    suspend fun ensureHealthyForPlay()

    suspend fun prewarmWebSession(forceUaRefresh: Boolean = false): Boolean
}

class NetworkManagerSecurityGateway : NetworkSecurityGateway {
    override suspend fun ensureHealthyForPlay() {
        NetworkManager.ensureHealthyForPlay()
    }

    override suspend fun prewarmWebSession(forceUaRefresh: Boolean): Boolean {
        return NetworkManager.prewarmWebSession(forceUaRefresh)
    }
}
