package com.legendsoftware.richman.backend.play

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.ProductPurchasesAcknowledgeRequest
import com.google.api.services.androidpublisher.model.SubscriptionPurchasesAcknowledgeRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.legendsoftware.richman.backend.model.AcknowledgementState
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.model.VerifiedPurchase
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.time.Instant

class GooglePlayPurchaseVerifier(
    private val publisher: AndroidPublisher,
) : PlayPurchaseVerifier {
    override fun verify(request: PurchaseSyncRequest, productId: String): VerifiedPurchase {
        return when (request.purchaseType) {
            PurchaseType.ONE_TIME -> verifyOneTimeProduct(request, productId)
            PurchaseType.SUBSCRIPTION -> verifySubscription(request, productId)
        }
    }

    override fun acknowledge(purchase: VerifiedPurchase) {
        when (purchase.purchaseType) {
            PurchaseType.ONE_TIME -> {
                publisher.purchases().products()
                    .acknowledge(
                        purchase.packageName,
                        purchase.productId,
                        purchase.purchaseToken,
                        ProductPurchasesAcknowledgeRequest(),
                    )
                    .execute()
            }

            PurchaseType.SUBSCRIPTION -> {
                publisher.purchases().subscriptions()
                    .acknowledge(
                        purchase.packageName,
                        purchase.productId,
                        purchase.purchaseToken,
                        SubscriptionPurchasesAcknowledgeRequest(),
                    )
                    .execute()
            }
        }
    }

    private fun verifyOneTimeProduct(request: PurchaseSyncRequest, productId: String): VerifiedPurchase {
        val purchase = publisher.purchases().products()
            .get(request.packageName, productId, request.purchaseToken)
            .execute()

        return VerifiedPurchase(
            packageName = request.packageName,
            productId = productId,
            purchaseToken = request.purchaseToken,
            orderId = purchase.orderId,
            purchaseType = PurchaseType.ONE_TIME,
            state = when (purchase.purchaseState) {
                0 -> PurchaseState.PURCHASED
                1 -> PurchaseState.CANCELED
                2 -> PurchaseState.PENDING
                else -> PurchaseState.UNKNOWN
            },
            acknowledgementState = when (purchase.acknowledgementState) {
                1 -> AcknowledgementState.ACKNOWLEDGED
                0 -> AcknowledgementState.NOT_ACKNOWLEDGED
                else -> AcknowledgementState.UNKNOWN
            },
            purchaseTime = purchase.purchaseTimeMillis?.let { Instant.ofEpochMilli(it) },
            rawResponse = purchase.toPrettyString(),
        )
    }

    private fun verifySubscription(request: PurchaseSyncRequest, productId: String): VerifiedPurchase {
        val purchase = publisher.purchases().subscriptionsv2()
            .get(request.packageName, request.purchaseToken)
            .execute()
        val lineItem = purchase.lineItems?.firstOrNull { it.productId == productId }
            ?: purchase.lineItems?.firstOrNull()
        val expiryTime = lineItem?.expiryTime?.let { Instant.parse(it) }

        return VerifiedPurchase(
            packageName = request.packageName,
            productId = lineItem?.productId ?: productId,
            purchaseToken = request.purchaseToken,
            orderId = purchase.latestOrderId,
            purchaseType = PurchaseType.SUBSCRIPTION,
            state = when (purchase.subscriptionState) {
                "SUBSCRIPTION_STATE_ACTIVE",
                "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
                "SUBSCRIPTION_STATE_ON_HOLD",
                "SUBSCRIPTION_STATE_PAUSED" -> PurchaseState.PURCHASED
                "SUBSCRIPTION_STATE_PENDING" -> PurchaseState.PENDING
                "SUBSCRIPTION_STATE_CANCELED",
                "SUBSCRIPTION_STATE_EXPIRED" -> PurchaseState.EXPIRED
                else -> PurchaseState.UNKNOWN
            },
            acknowledgementState = when (purchase.acknowledgementState) {
                "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED" -> AcknowledgementState.ACKNOWLEDGED
                "ACKNOWLEDGEMENT_STATE_PENDING" -> AcknowledgementState.NOT_ACKNOWLEDGED
                else -> AcknowledgementState.UNKNOWN
            },
            purchaseTime = purchase.startTime?.let { Instant.parse(it) },
            expiryTime = expiryTime,
            autoRenewing = purchase.lineItems?.any { it.autoRenewingPlan != null },
            rawResponse = purchase.toPrettyString(),
        )
    }

    companion object {
        fun fromEnvironment(): PlayPurchaseVerifier {
            val allowUnverified = System.getenv("RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES") == "true"
            val credentials = loadCredentials()
            if (credentials == null) {
                if (allowUnverified) {
                    return EnvironmentPlayPurchaseVerifier()
                }
                error(
                    "Google Play verification is not configured. Set PLAY_SERVICE_ACCOUNT_JSON or " +
                        "GOOGLE_APPLICATION_CREDENTIALS, or enable local mock verification with " +
                        "RICHMAN_ALLOW_UNVERIFIED_PLAY_PURCHASES=true."
                )
            }

            val scopedCredentials = credentials.createScoped(AndroidPublisherScopes.ANDROIDPUBLISHER)
            return GooglePlayPurchaseVerifier(
                publisher = buildPublisher(
                    transport = GoogleNetHttpTransport.newTrustedTransport(),
                    requestInitializer = HttpCredentialsAdapter(scopedCredentials),
                )
            )
        }

        private fun loadCredentials(): GoogleCredentials? {
            val json = System.getenv("PLAY_SERVICE_ACCOUNT_JSON")?.takeIf { it.isNotBlank() }
            if (json != null) {
                return GoogleCredentials.fromStream(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
            }

            val path = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")?.takeIf { it.isNotBlank() }
            if (path != null) {
                return FileInputStream(path).use { GoogleCredentials.fromStream(it) }
            }

            return null
        }

        private fun buildPublisher(
            transport: HttpTransport,
            requestInitializer: HttpRequestInitializer,
        ): AndroidPublisher = AndroidPublisher.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            requestInitializer,
        )
            .setApplicationName("Richman Backend")
            .build()
    }
}
