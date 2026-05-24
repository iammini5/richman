package com.legendsoftware.richman.backend.model

import java.time.Instant
import java.util.UUID

const val RICHMAN_PACKAGE_NAME = "com.legendsoftware.richman"

data class PurchaseSyncRequest(
    val userId: String,
    val packageName: String,
    val purchaseType: PurchaseType,
    val productIds: List<String>,
    val purchaseToken: String,
    val appVersion: String? = null,
    val clientPurchaseTime: Instant? = null,
)

data class PurchaseSyncResponse(
    val status: SyncStatus,
    val entitlements: EntitlementSnapshot,
)

data class EntitlementSnapshot(
    val coins: Int,
    val premiumTier: String?,
    val premiumState: PremiumState,
    val premiumExpiresAt: Instant?,
)

data class VerifiedPurchase(
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val orderId: String?,
    val purchaseType: PurchaseType,
    val state: PurchaseState,
    val acknowledgementState: AcknowledgementState,
    val purchaseTime: Instant?,
    val expiryTime: Instant? = null,
    val autoRenewing: Boolean? = null,
    val rawResponse: String? = null,
)

data class PurchaseRecord(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val packageName: String,
    val productId: String,
    val purchaseTokenHash: String,
    val orderId: String?,
    val purchaseType: PurchaseType,
    val state: PurchaseState,
    val acknowledgementState: AcknowledgementState,
    val purchaseTime: Instant?,
    val expiryTime: Instant?,
    val lastVerifiedAt: Instant,
    val rawResponse: String?,
)

data class EntitlementLedgerEntry(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val purchaseId: UUID,
    val productId: String,
    val type: EntitlementType,
    val key: String,
    val amount: Int,
    val state: EntitlementState,
    val reason: String,
    val createdAt: Instant = Instant.now(),
)

enum class PurchaseType {
    ONE_TIME,
    SUBSCRIPTION,
}

enum class PurchaseState {
    PURCHASED,
    PENDING,
    CANCELED,
    EXPIRED,
    UNKNOWN,
}

enum class AcknowledgementState {
    ACKNOWLEDGED,
    NOT_ACKNOWLEDGED,
    UNKNOWN,
}

enum class EntitlementType {
    COINS,
    BUNDLE,
    PREMIUM,
}

enum class EntitlementState {
    GRANTED,
    REVOKED,
    ACTIVE,
    EXPIRED,
}

enum class PremiumState {
    NONE,
    ACTIVE,
    EXPIRED,
}

enum class SyncStatus {
    VERIFIED,
    PENDING,
    REJECTED,
}
