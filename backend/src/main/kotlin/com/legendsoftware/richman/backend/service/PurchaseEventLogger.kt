package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.model.PurchaseRecord
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.RtdnEvent
import com.legendsoftware.richman.backend.model.RtdnEventType
import com.legendsoftware.richman.backend.model.VerifiedPurchase
import java.util.logging.Logger

object PurchaseEventLogger {
    private val logger: Logger = Logger.getLogger(PurchaseEventLogger::class.java.name)

    fun logVerifiedPurchase(
        source: String,
        request: PurchaseSyncRequest,
        verified: VerifiedPurchase,
        saved: PurchaseRecord,
        acknowledged: Boolean,
    ) {
        logger.info(
            json(
                "event" to "play_purchase_verified",
                "source" to source,
                "accountClass" to verified.accountClass(),
                "packageName" to verified.packageName,
                "productId" to verified.productId,
                "purchaseType" to verified.purchaseType.name,
                "purchaseState" to verified.state.name,
                "acknowledgementState" to verified.acknowledgementState.name,
                "acknowledgedByBackend" to acknowledged,
                "orderId" to verified.orderId.maskOrderId(),
                "purchaseTokenHashPrefix" to saved.purchaseTokenHash.take(12),
                "userIdHashPrefix" to request.userId.hashCode().toUInt().toString(16),
                "appVersion" to request.appVersion,
                "purchaseTime" to verified.purchaseTime?.toString(),
                "expiryTime" to verified.expiryTime?.toString(),
                "autoRenewing" to verified.autoRenewing,
            )
        )
    }

    fun logRtdnReceived(
        notification: RtdnNotification,
        messageId: String?,
        tokenHash: String?,
        hasExistingPurchase: Boolean,
        willReverify: Boolean,
    ) {
        logger.info(
            json(
                "event" to "play_rtdn_received",
                "messageId" to messageId,
                "packageName" to notification.packageName,
                "rtdnType" to notification.eventType.name,
                "productId" to notification.productId,
                "notificationType" to notification.notificationType,
                "purchaseTokenHashPrefix" to tokenHash?.take(12),
                "hasExistingPurchase" to hasExistingPurchase,
                "willReverify" to willReverify,
                "isTestNotification" to (notification.eventType == RtdnEventType.TEST),
            )
        )
    }

    fun logRtdnStored(event: RtdnEvent) {
        logger.info(
            json(
                "event" to "play_rtdn_stored",
                "messageId" to event.messageId,
                "packageName" to event.packageName,
                "rtdnType" to event.eventType.name,
                "productId" to event.productId,
                "notificationType" to event.notificationType,
                "status" to event.status.name,
                "purchaseTokenHashPrefix" to event.purchaseTokenHash?.take(12),
            )
        )
    }

    fun logDuplicateRtdn(messageId: String?) {
        logger.info(
            json(
                "event" to "play_rtdn_duplicate",
                "messageId" to messageId,
            )
        )
    }

    private fun VerifiedPurchase.accountClass(): String {
        val raw = rawResponse.orEmpty()
        return when {
            raw.contains("testPurchase", ignoreCase = true) -> "TEST"
            raw.contains("\"purchaseType\" : 0") || raw.contains("\"purchaseType\":0") -> "TEST"
            else -> "REAL_OR_UNKNOWN"
        }
    }

    private fun String?.maskOrderId(): String? {
        if (isNullOrBlank()) return null
        val visibleSuffix = takeLast(6)
        return "***$visibleSuffix"
    }

    private fun json(vararg pairs: Pair<String, Any?>): String =
        pairs.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${key.escapeJson()}\":${value.jsonValue()}"
        }

    private fun Any?.jsonValue(): String = when (this) {
        null -> "null"
        is Boolean -> toString()
        is Number -> toString()
        else -> "\"${toString().escapeJson()}\""
    }

    private fun String.escapeJson(): String =
        buildString {
            this@escapeJson.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
}
