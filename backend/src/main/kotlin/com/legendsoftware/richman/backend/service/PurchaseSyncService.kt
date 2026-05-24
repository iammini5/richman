package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.catalog.ProductCatalog
import com.legendsoftware.richman.backend.model.AcknowledgementState
import com.legendsoftware.richman.backend.model.PurchaseRecord
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.PurchaseSyncRequest
import com.legendsoftware.richman.backend.model.PurchaseSyncResponse
import com.legendsoftware.richman.backend.model.RICHMAN_PACKAGE_NAME
import com.legendsoftware.richman.backend.model.SyncStatus
import com.legendsoftware.richman.backend.play.PlayPurchaseVerifier
import com.legendsoftware.richman.backend.store.PurchaseRepository
import com.legendsoftware.richman.backend.util.sha256
import java.time.Instant

class PurchaseSyncService(
    private val repository: PurchaseRepository,
    private val verifier: PlayPurchaseVerifier,
    private val catalog: ProductCatalog,
    private val entitlementService: EntitlementService,
) {
    fun sync(request: PurchaseSyncRequest): PurchaseSyncResponse {
        require(request.packageName == RICHMAN_PACKAGE_NAME) {
            "Unsupported package name: ${request.packageName}"
        }
        require(request.userId.isNotBlank()) { "userId is required" }
        require(request.purchaseToken.isNotBlank()) { "purchaseToken is required" }
        require(request.productIds.isNotEmpty()) { "At least one productId is required" }

        request.productIds.forEach { productId ->
            catalog.requireRule(productId)
            val verified = verifier.verify(request, productId)
            require(verified.packageName == request.packageName) { "Verified package does not match request" }
            require(verified.productId == productId) { "Verified product does not match request" }

            if (verified.state == PurchaseState.PURCHASED &&
                verified.acknowledgementState == AcknowledgementState.NOT_ACKNOWLEDGED
            ) {
                verifier.acknowledge(verified)
            }

            val record = PurchaseRecord(
                userId = request.userId,
                packageName = verified.packageName,
                productId = verified.productId,
                purchaseTokenHash = verified.purchaseToken.sha256(),
                orderId = verified.orderId,
                purchaseType = verified.purchaseType,
                state = verified.state,
                acknowledgementState = AcknowledgementState.ACKNOWLEDGED,
                purchaseTime = verified.purchaseTime,
                expiryTime = verified.expiryTime,
                lastVerifiedAt = Instant.now(),
                rawResponse = verified.rawResponse,
            )
            val saved = repository.upsertPurchase(record)

            if (saved.state == PurchaseState.PURCHASED) {
                entitlementService.grantForPurchase(saved)
            }
        }

        return PurchaseSyncResponse(
            status = SyncStatus.VERIFIED,
            entitlements = entitlementService.snapshotForUser(request.userId),
        )
    }
}
