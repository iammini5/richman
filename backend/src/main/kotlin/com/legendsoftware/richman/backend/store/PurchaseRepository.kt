package com.legendsoftware.richman.backend.store

import com.legendsoftware.richman.backend.model.EntitlementLedgerEntry
import com.legendsoftware.richman.backend.model.PurchaseRecord
import java.util.UUID

interface PurchaseRepository {
    fun upsertPurchase(record: PurchaseRecord): PurchaseRecord
    fun purchaseById(id: UUID): PurchaseRecord?
    fun hasGrantForPurchase(purchaseId: UUID): Boolean
    fun saveLedgerEntry(entry: EntitlementLedgerEntry)
    fun ledgerForUser(userId: String): List<EntitlementLedgerEntry>
}

class InMemoryPurchaseRepository : PurchaseRepository {
    private val purchasesById = linkedMapOf<UUID, PurchaseRecord>()
    private val purchaseIdsByTokenAndProduct = linkedMapOf<String, UUID>()
    private val ledgerEntries = mutableListOf<EntitlementLedgerEntry>()

    @Synchronized
    override fun upsertPurchase(record: PurchaseRecord): PurchaseRecord {
        val key = "${record.purchaseTokenHash}:${record.productId}"
        val existingId = purchaseIdsByTokenAndProduct[key]
        val saved = if (existingId == null) {
            record
        } else {
            val existing = purchasesById.getValue(existingId)
            record.copy(id = existing.id)
        }

        purchasesById[saved.id] = saved
        purchaseIdsByTokenAndProduct[key] = saved.id
        return saved
    }

    @Synchronized
    override fun purchaseById(id: UUID): PurchaseRecord? = purchasesById[id]

    @Synchronized
    override fun hasGrantForPurchase(purchaseId: UUID): Boolean =
        ledgerEntries.any { it.purchaseId == purchaseId }

    @Synchronized
    override fun saveLedgerEntry(entry: EntitlementLedgerEntry) {
        if (!hasGrantForPurchase(entry.purchaseId)) {
            ledgerEntries += entry
        }
    }

    @Synchronized
    override fun ledgerForUser(userId: String): List<EntitlementLedgerEntry> =
        ledgerEntries.filter { it.userId == userId }
}
