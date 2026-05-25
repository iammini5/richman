package com.legendsoftware.richmangoogleplaybillinglibrarytest.backend

import com.android.billingclient.api.Purchase
import com.legendsoftware.richmangoogleplaybillinglibrarytest.BuildConfig
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

class PurchaseSyncClient {
    fun syncPurchase(purchase: Purchase): Result<String> = runCatching {
        val productIds = purchase.products
        require(productIds.isNotEmpty()) { "Purchase has no product IDs." }

        val connection = (URL("${BuildConfig.RICHMAN_BACKEND_URL}/v1/play/purchases:sync").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (BuildConfig.RICHMAN_BACKEND_API_KEY.isNotBlank()) {
                setRequestProperty("X-Richman-Api-Key", BuildConfig.RICHMAN_BACKEND_API_KEY)
            }
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(buildRequestJson(purchase, productIds))
        }

        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Backend purchase sync failed: HTTP $responseCode $responseBody")
        }

        responseBody
    }

    private fun buildRequestJson(purchase: Purchase, productIds: List<String>): String {
        val purchaseType = if (productIds.any { it.isSubscriptionProductId() }) {
            "subscription"
        } else {
            "one_time"
        }
        val productJson = productIds.joinToString(separator = ",") { """"${it.jsonEscape()}"""" }
        return """
            {
              "userId": "${stableUserId().jsonEscape()}",
              "packageName": "com.legendsoftware.richman",
              "purchaseType": "$purchaseType",
              "productIds": [$productJson],
              "purchaseToken": "${purchase.purchaseToken.jsonEscape()}",
              "clientPurchaseTime": "${Instant.ofEpochMilli(purchase.purchaseTime)}",
              "appVersion": "${BuildConfig.VERSION_NAME.jsonEscape()}"
            }
        """.trimIndent()
    }

    private fun stableUserId(): String {
        val configured = BuildConfig.APPLICATION_ID
        return UUID.nameUUIDFromBytes(configured.toByteArray(Charsets.UTF_8)).toString()
    }

    private fun String.isSubscriptionProductId(): Boolean =
        this == PREMIUM_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID

    private fun String.jsonEscape(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")
}
