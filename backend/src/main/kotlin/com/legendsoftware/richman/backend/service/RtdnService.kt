package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.model.RICHMAN_PACKAGE_NAME
import com.legendsoftware.richman.backend.model.RtdnEvent
import com.legendsoftware.richman.backend.model.RtdnEventType
import com.legendsoftware.richman.backend.model.RtdnProcessingStatus
import com.legendsoftware.richman.backend.store.PurchaseRepository
import com.legendsoftware.richman.backend.util.sha256
import java.time.Instant
import java.util.Base64

class RtdnService(
    private val repository: PurchaseRepository,
    private val purchaseSyncService: PurchaseSyncService,
) {
    fun processPubSubPush(body: String): RtdnProcessResult {
        val messageId = RtdnJson.string("messageId", body)
        if (messageId != null && repository.hasRtdnMessage(messageId)) {
            PurchaseEventLogger.logDuplicateRtdn(messageId)
            return RtdnProcessResult(RtdnProcessingStatus.DUPLICATE, null)
        }

        val encodedData = RtdnJson.string("data", body) ?: error("Pub/Sub message data is required")
        val decoded = String(Base64.getDecoder().decode(encodedData), Charsets.UTF_8)
        val notification = RtdnNotification.fromJson(decoded)
        require(notification.packageName == RICHMAN_PACKAGE_NAME) {
            "Unsupported package name: ${notification.packageName}"
        }

        val tokenHash = notification.purchaseToken?.sha256()
        val existingPurchase = tokenHash?.let { repository.purchaseByTokenHash(it) }
        val eventType = notification.eventType

        if (eventType == RtdnEventType.VOIDED_PURCHASE && tokenHash != null) {
            repository.updatePurchaseStateByTokenHash(tokenHash, PurchaseState.CANCELED)
        }

        val canReverify = existingPurchase != null &&
            eventType != RtdnEventType.TEST &&
            eventType != RtdnEventType.UNKNOWN &&
            eventType != RtdnEventType.VOIDED_PURCHASE

        PurchaseEventLogger.logRtdnReceived(
            notification = notification,
            messageId = messageId,
            tokenHash = tokenHash,
            hasExistingPurchase = existingPurchase != null,
            willReverify = canReverify,
        )

        if (canReverify) {
            val productId = notification.productId ?: existingPurchase!!.productId
            val purchaseType = when (eventType) {
                RtdnEventType.ONE_TIME_PRODUCT -> PurchaseType.ONE_TIME
                RtdnEventType.SUBSCRIPTION -> PurchaseType.SUBSCRIPTION
                else -> existingPurchase!!.purchaseType
            }
            purchaseSyncService.sync(
                PurchaseSyncRequest(
                    userId = existingPurchase!!.userId,
                    packageName = notification.packageName,
                    purchaseType = purchaseType,
                    productIds = listOf(productId),
                    purchaseToken = notification.purchaseToken!!,
                    appVersion = "rtdn",
                    clientPurchaseTime = Instant.now(),
                )
            )
        }

        val status = if (canReverify || eventType == RtdnEventType.VOIDED_PURCHASE || eventType == RtdnEventType.TEST) {
            RtdnProcessingStatus.PROCESSED
        } else {
            RtdnProcessingStatus.RECORDED_ONLY
        }

        val event = repository.saveRtdnEvent(
            RtdnEvent(
                messageId = messageId,
                packageName = notification.packageName,
                eventType = eventType,
                purchaseTokenHash = tokenHash,
                productId = notification.productId ?: existingPurchase?.productId,
                notificationType = notification.notificationType,
                status = status,
                rawPayload = decoded,
            )
        )
        PurchaseEventLogger.logRtdnStored(event)

        return RtdnProcessResult(status, event)
    }
}

data class RtdnProcessResult(
    val status: RtdnProcessingStatus,
    val event: RtdnEvent?,
)

data class RtdnNotification(
    val packageName: String,
    val eventType: RtdnEventType,
    val purchaseToken: String?,
    val productId: String?,
    val notificationType: Int?,
) {
    companion object {
        fun fromJson(json: String): RtdnNotification {
            val packageName = RtdnJson.string("packageName", json) ?: error("packageName is required")
            val subscriptionToken = RtdnJson.objectString("subscriptionNotification", "purchaseToken", json)
            val subscriptionId = RtdnJson.objectString("subscriptionNotification", "subscriptionId", json)
            val subscriptionNotificationType = RtdnJson.objectInt(
                "subscriptionNotification",
                "notificationType",
                json,
            )
            val oneTimeToken = RtdnJson.objectString("oneTimeProductNotification", "purchaseToken", json)
            val sku = RtdnJson.objectString("oneTimeProductNotification", "sku", json)
            val oneTimeNotificationType = RtdnJson.objectInt(
                "oneTimeProductNotification",
                "notificationType",
                json,
            )
            val voidedToken = RtdnJson.objectString("voidedPurchaseNotification", "purchaseToken", json)

            return when {
                subscriptionToken != null -> RtdnNotification(
                    packageName = packageName,
                    eventType = RtdnEventType.SUBSCRIPTION,
                    purchaseToken = subscriptionToken,
                    productId = subscriptionId,
                    notificationType = subscriptionNotificationType,
                )

                oneTimeToken != null -> RtdnNotification(
                    packageName = packageName,
                    eventType = RtdnEventType.ONE_TIME_PRODUCT,
                    purchaseToken = oneTimeToken,
                    productId = sku,
                    notificationType = oneTimeNotificationType,
                )

                voidedToken != null -> RtdnNotification(
                    packageName = packageName,
                    eventType = RtdnEventType.VOIDED_PURCHASE,
                    purchaseToken = voidedToken,
                    productId = null,
                    notificationType = RtdnJson.objectInt("voidedPurchaseNotification", "refundType", json),
                )

                RtdnJson.objectString("testNotification", "version", json) != null -> RtdnNotification(
                    packageName = packageName,
                    eventType = RtdnEventType.TEST,
                    purchaseToken = null,
                    productId = null,
                    notificationType = null,
                )

                else -> RtdnNotification(
                    packageName = packageName,
                    eventType = RtdnEventType.UNKNOWN,
                    purchaseToken = null,
                    productId = null,
                    notificationType = null,
                )
            }
        }
    }
}

object RtdnJson {
    fun string(name: String, json: String): String? {
        val pattern = Regex(""""$name"\s*:\s*"([^"]*)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    fun objectString(objectName: String, fieldName: String, json: String): String? =
        objectBody(objectName, json)?.let { string(fieldName, it) }

    fun objectInt(objectName: String, fieldName: String, json: String): Int? =
        objectBody(objectName, json)?.let { body ->
            Regex(""""$fieldName"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toInt()
        }

    private fun objectBody(name: String, json: String): String? {
        val start = Regex(""""$name"\s*:\s*\{""").find(json)?.range?.last ?: return null
        var depth = 1
        var index = start + 1
        while (index < json.length) {
            when (json[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return json.substring(start + 1, index)
                }
            }
            index += 1
        }
        return null
    }
}
