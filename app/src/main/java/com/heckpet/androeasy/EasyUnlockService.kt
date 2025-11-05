package com.heckpet.androeasy

import android.service.trust.TrustAgentService

class EasyUnlockService : TrustAgentService() {

    override fun onCreate() {
        super.onCreate()
        setManagingTrust(true)
    }
}