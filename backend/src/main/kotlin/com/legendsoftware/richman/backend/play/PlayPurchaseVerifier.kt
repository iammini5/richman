package com.legendsoftware.richman.backend.play

import com.legendsoftware.richman.backend.model.AcknowledgementState
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.model.VerifiedPurchase
import java.time.Instant

interface PlayPurchaseVerifier {
    fun verify(request: PurchaseSyncRequest, productId: String): VerifiedPurchase

    fun acknowledge(purchase: VerifiedPurchase) {
        // Production implementations should call the Google Play acknowledgement endpoint.
    }
}

class EnvironmentPlayPurchaseVerifier : PlayPurchaseVerifier {
    override fun verify(request: PurchaseSyncRequest, productId: String): VerifiedPurchase {
        val allowUnverified = System.getenv("RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES") == "true"
        if (!allowUnverified) {
            error(
                "Google Play verification is not configured. Set RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES=true " +
                    "for local mock testing, or provide a production PlayPurchaseVerifier."
            )
        }

        return VerifiedPurchase(
            packageName = request.packageName,
            productId = productId,
            purchaseToken = request.purchaseToken,
            orderId = "local-${request.purchaseToken.takeLast(8)}-$productId",
            purchaseType = request.purchaseType,
            state = PurchaseState.PURCHASED,
            acknowledgementState = AcknowledgementState.NOT_ACKNOWLEDGED,
            purchaseTime = request.clientPurchaseTime ?: Instant.now(),
            expiryTime = if (request.purchaseType == PurchaseType.SUBSCRIPTION) {
                Instant.now().plusSeconds(30L * 24L * 60L * 60L)
            } else {
                null
            },
            autoRenewing = request.purchaseType == PurchaseType.SUBSCRIPTION,
            rawResponse = """{"source":"local-env-verifier"}""",
        )
    }

    override fun acknowledge(purchase: VerifiedPurchase) {
        // The local verifier accepts acknowledgement without external calls.
    }
}
