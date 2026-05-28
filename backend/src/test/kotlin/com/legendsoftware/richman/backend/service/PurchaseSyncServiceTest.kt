package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.RichmanBackendFactory
import com.legendsoftware.richman.backend.model.AcknowledgementState
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseType
import com.legendsoftware.richman.backend.model.RICHMAN_PACKAGE_NAME
import com.legendsoftware.richman.backend.model.VerifiedPurchase
import com.legendsoftware.richman.backend.play.PlayPurchaseVerifier
import com.legendsoftware.richman.backend.store.InMemoryPurchaseRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PurchaseSyncServiceTest {
    @Test
    fun `coin purchase grants coins once when sync is retried`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = FakeVerifier(),
        )
        val request = syncRequest(
            purchaseType = PurchaseType.ONE_TIME,
            productIds = listOf("com.legendsoftware.richman.coins.100"),
            purchaseToken = "token-100",
        )

        val first = services.purchaseSyncService.sync(request)
        val second = services.purchaseSyncService.sync(request)

        assertEquals(100, first.entitlements.coins)
        assertEquals(100, second.entitlements.coins)
    }

    @Test
    fun `highest active subscription tier is returned`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = FakeVerifier(),
        )

        services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.SUBSCRIPTION,
                productIds = listOf("premium_basic_monthly"),
                purchaseToken = "basic-token",
            )
        )
        val response = services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.SUBSCRIPTION,
                productIds = listOf("premium_pro_monthly"),
                purchaseToken = "pro-token",
            )
        )

        assertEquals("pro", response.entitlements.premiumTier)
        assertTrue(response.entitlements.premiumExpiresAt!!.isAfter(Instant.now()))
    }

    @Test
    fun `new premium subscription product ids grant expected tiers`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = FakeVerifier(),
        )

        services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.SUBSCRIPTION,
                productIds = listOf("premium_basic"),
                purchaseToken = "new-basic-token",
            )
        )
        services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.SUBSCRIPTION,
                productIds = listOf("premium_plus"),
                purchaseToken = "new-plus-token",
            )
        )
        val response = services.purchaseSyncService.sync(
            syncRequest(
                purchaseType = PurchaseType.SUBSCRIPTION,
                productIds = listOf("premium_pro"),
                purchaseToken = "new-pro-token",
            )
        )

        assertEquals("pro", response.entitlements.premiumTier)
        assertTrue(response.entitlements.premiumExpiresAt!!.isAfter(Instant.now()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported package is rejected`() {
        val services = RichmanBackendFactory.create(
            repository = InMemoryPurchaseRepository(),
            verifier = FakeVerifier(),
        )

        services.purchaseSyncService.sync(
            syncRequest(
                packageName = "com.other.app",
                purchaseType = PurchaseType.ONE_TIME,
                productIds = listOf("com.legendsoftware.richman.coins.50"),
                purchaseToken = "token",
            )
        )
    }

    private fun syncRequest(
        packageName: String = RICHMAN_PACKAGE_NAME,
        purchaseType: PurchaseType,
        productIds: List<String>,
        purchaseToken: String,
    ): PurchaseSyncRequest = PurchaseSyncRequest(
        userId = "user-1",
        packageName = packageName,
        purchaseType = purchaseType,
        productIds = productIds,
        purchaseToken = purchaseToken,
        clientPurchaseTime = Instant.parse("2026-05-24T00:00:00Z"),
        appVersion = "1.0",
    )
}

private class FakeVerifier : PlayPurchaseVerifier {
    override fun verify(request: PurchaseSyncRequest, productId: String): VerifiedPurchase =
        VerifiedPurchase(
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
