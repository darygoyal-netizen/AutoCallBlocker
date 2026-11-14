package com.harshit.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService

class SpamBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""

        val response = CallResponse.Builder()
        if (BlockHelper.shouldBlockNumber(this, rawNumber)) {
            // नंबर को परमानेंट ब्लॉक करें और हिस्ट्री में सेव करें
            BlockHelper.recordAndPermanentlyBlock(this, rawNumber)

            // 0ms में बिना स्क्रीन जलाए और बिना रिंग बजाए तुरंत कट करें
            response.setDisallowCall(true)
                .setRejectCall(true)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
        } else {
            response.setDisallowCall(false)
        }

        respondToCall(callDetails, response.build())
    }
}
