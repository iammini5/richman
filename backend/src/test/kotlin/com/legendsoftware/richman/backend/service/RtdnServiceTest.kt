package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.RichmanBackendFactory
import com.legendsoftware.richman.backend.model.AcknowledgementState
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.model.RICHMAN_PACKAGE_NAME
import com.legendsoftware.richman.backend.model.RtdnProcessingStatus
import com.legendsoftware.richman.backend.model.VerifiedPurchase
import com.legendsoftware.richman.backend.play.PlayPurchaseVerifier
import com.legendsoftware.richman.backend.store.InMemoryPurchaseRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Base64

class RtdnServiceTest {
    @Test
    fun `known one-time product notification re-verifies without duplicate grant`() {
        val verifier = CountingVerifier()
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = verifier,
        )

        services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.ONE_TIME,
                productIds = listOf("com.legendsoftware.richman.coins.100"),
                purchaseToken = "token-100",
            )
        )

        val result = services.rtdnService.processPubSubPush(
            pubSubPush(
                messageId = "message-1",
                decodedPayload = oneTimeProductPayload(
                    purchaseToken = "token-100",
                    productId = "com.legendsoftware.richman.coins.100",
                ),
            )
        )
        val snapshot = services.entitlementService.snapshotForUser("user-1")

        assertEquals(RtdnProcessingStatus.PROCESSED, result.status)
        assertEquals(2, verifier.verifyCount)
        assertEquals(100, snapshot.coins)
    }

    @Test
    fun `voided purchase notification removes purchase from entitlement snapshot`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = CountingVerifier(),
        )

        services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.ONE_TIME,
                productIds = listOf("com.legendsoftware.richman.coins.100"),
                purchaseToken = "token-100",
            )
        )

        val result = services.rtdnService.processPubSubPush(
            pubSubPush(
                messageId = "message-void",
                decodedPayload = voidedPurchasePayload(purchaseToken = "token-100"),
            )
        )
        val snapshot = services.entitlementService.snapshotForUser("user-1")

        assertEquals(RtdnProcessingStatus.PROCESSED, result.status)
        assertEquals(0, snapshot.coins)
    }

    @Test
    fun `duplicate PubSub message is ignored`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = CountingVerifier(),
        )
        val body = pubSubPush(
            messageId = "message-duplicate",
            decodedPayload = oneTimeProductPayload(
                purchaseToken = "unknown-token",
                productId = "com.legendsoftware.richman.coins.100",
            ),
        )

        val first = services.rtdnService.processPubSubPush(body)
        val second = services.rtdnService.processPubSubPush(body)

        assertEquals(RtdnProcessingStatus.PROCESSED, first.status)
        assertEquals(RtdnProcessingStatus.DUPLICATE, second.status)
    }

    @Test
    fun `unknown one-time product notification verifies and grants fallback user`() {
        val verifier = CountingVerifier()
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = verifier,
        )

        val result = services.rtdnService.processPubSubPush(
            pubSubPush(
                messageId = "message-unknown-one-time",
                decodedPayload = oneTimeProductPayload(
                    purchaseToken = "unknown-token",
                    productId = "com.legendsoftware.richman.coins.50",
                ),
            )
        )
        val snapshot = services.entitlementService.snapshotForUser(RtdnService.FALLBACK_RTDN_USER_ID)

        assertEquals(RtdnProcessingStatus.PROCESSED, result.status)
        assertEquals(1, verifier.verifyCount)
        assertEquals(50, snapshot.coins)
    }

    private fun syncRequest(
        purchaseType: PurchaseType,
        productIds: List<String>,
        purchaseToken: String,
    ): PurchaseSyncRequest = PurchaseSyncRequest(
        userId = "user-1",
        packageName = RICHMAN_PACKAGE_NAME,
        purchaseType = purchaseType,
        productIds = productIds,
        purchaseToken = purchaseToken,
        clientPurchaseTime = Instant.parse("2026-05-24T00:00:00Z"),
        appVersion = "1.0",
    )

    private fun pubSubPush(messageId: String, decodedPayload: String): String {
        val encoded = Base64.getEncoder().encodeToString(decodedPayload.toByteArray(Charsets.UTF_8))
        return """{"message":{"messageId":"$messageId","data":"$encoded"}}"""
    }

    private fun oneTimeProductPayload(purchaseToken: String, productId: String): String =
        """
            {
              "version": "1.0",
              "packageName": "$RICHMAN_PACKAGE_NAME",
              "eventTimeMillis": "1779580800000",
              "oneTimeProductNotification": {
                "version": "1.0",
                "notificationType": 1,
                "purchaseToken": "$purchaseToken",
                "sku": "$productId"
              }
            }
        """.trimIndent()

    private fun voidedPurchasePayload(purchaseToken: String): String =
        """
            {
              "version": "1.0",
              "packageName": "$RICHMAN_PACKAGE_NAME",
              "eventTimeMillis": "1779580800000",
              "voidedPurchaseNotification": {
                "purchaseToken": "$purchaseToken",
                "orderId": "order-token-100",
                "productType": 1,
                "refundType": 1
              }
            }
        """.trimIndent()
}

private class CountingVerifier : PlayPurchaseVerifier {
    var verifyCount: Int = 0

    override fun verify(request: PurchaseSyncRequest, productId: String): VerifiedPurchase {
        verifyCount += 1
        return VerifiedPurchase(
            packageName = request.packageName,
            productId = productId,
            purchaseToken = request.purchaseToken,
            orderId = "order-${request.purchaseToken}-$productId",
            purchaseType = request.purchaseType,
            state = PurchaseState.PURCHASED,
            acknowledgementState = AcknowledgementState.NOT_ACKNOWLEDGED,
            purchaseTime = request.clientPurchaseTime,
            expiryTime = if (request.purchaseType == PurchaseType.SUBSCRIPTION) {
                Instant.now().plusSeconds(2_592_000)
            } else {
                null
            },
            autoRenewing = request.purchaseType == PurchaseType.SUBSCRIPTION,
            rawResponse = "{}",
        )
    }
}
