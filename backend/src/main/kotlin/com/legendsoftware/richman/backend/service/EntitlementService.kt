package com.legendsoftware.richman.backend.service

import com.legendsoftware.richman.backend.catalog.ProductCatalog
import com.legendsoftware.richman.backend.catalog.ProductRule
import com.legendsoftware.richman.backend.model.EntitlementLedgerEntry
import com.legendsoftware.richman.backend.model.EntitlementSnapshot
import com.legendsoftware.richman.backend.model.EntitlementState
import com.legendsoftware.richman.backend.model.EntitlementType
import com.legendsoftware.richman.backend.model.PremiumState
import com.legendsoftware.richman.backend.model.PurchaseRecord
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.store.PurchaseRepository

class EntitlementService(
    private val repository: PurchaseRepository,
    private val catalog: ProductCatalog,
) {
    fun grantForPurchase(record: PurchaseRecord) {
        if (repository.hasGrantForPurchase(record.id)) return

        when (val rule = catalog.requireRule(record.productId)) {
            is ProductRule.OneTimeCoins -> {
                repository.saveLedgerEntry(
                    EntitlementLedgerEntry(
                        userId = record.userId,
                        purchaseId = record.id,
                        productId = record.productId,
                        type = EntitlementType.COINS,
                        key = "balance",
                        amount = rule.coinAmount,
                        state = EntitlementState.GRANTED,
                        reason = "purchase_verified",
                    )
                )
            }

            is ProductRule.StarterBundle -> {
                repository.saveLedgerEntry(
                    EntitlementLedgerEntry(
                        userId = record.userId,
                        purchaseId = record.id,
                        productId = record.productId,
                        type = EntitlementType.COINS,
                        key = "balance",
                        amount = rule.coinAmount,
                        state = EntitlementState.GRANTED,
                        reason = rule.entitlementKey,
                    )
                )
            }

            is ProductRule.Subscription -> {
                repository.saveLedgerEntry(
                    EntitlementLedgerEntry(
                        userId = record.userId,
                        purchaseId = record.id,
                        productId = record.productId,
                        type = EntitlementType.PREMIUM,
                        key = rule.tier,
                        amount = rule.priority,
                        state = EntitlementState.ACTIVE,
                        reason = "subscription_verified",
                    )
                )
            }
        }
    }

    fun snapshotForUser(userId: String): EntitlementSnapshot {
        val entries = repository.ledgerForUser(userId)
        val coins = entries
            .filter {
                it.type == EntitlementType.COINS &&
                    it.state == EntitlementState.GRANTED &&
                    repository.purchaseById(it.purchaseId)?.state == PurchaseState.PURCHASED
            }
            .sumOf { it.amount }

        val activePremium = entries
            .filter {
                it.type == EntitlementType.PREMIUM &&
                    it.state == EntitlementState.ACTIVE &&
                    repository.purchaseById(it.purchaseId)?.state == PurchaseState.PURCHASED
            }
            .maxByOrNull { it.amount }

        val premiumRecord = activePremium?.let { repository.purchaseById(it.purchaseId) }

        return EntitlementSnapshot(
            coins = coins,
            premiumTier = activePremium?.key,
            premiumState = if (activePremium == null) PremiumState.NONE else PremiumState.ACTIVE,
            premiumExpiresAt = premiumRecord?.expiryTime,
        )
    }
}
