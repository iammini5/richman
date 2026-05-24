package com.legendsoftware.richman.backend.store

import com.legendsoftware.richman.backend.model.EntitlementLedgerEntry
import com.legendsoftware.richman.backend.model.PurchaseRecord
import com.legendsoftware.richman.backend.model.PurchaseState
import com.legendsoftware.richman.backend.model.RtdnEvent
import java.util.UUID

interface PurchaseRepository {
    fun upsertPurchase(record: PurchaseRecord): PurchaseRecord
    fun purchaseById(id: UUID): PurchaseRecord?
    fun purchaseByTokenHash(purchaseTokenHash: String): PurchaseRecord?
    fun updatePurchaseStateByTokenHash(purchaseTokenHash: String, state: PurchaseState): PurchaseRecord?
    fun hasGrantForPurchase(purchaseId: UUID): Boolean
    fun saveLedgerEntry(entry: EntitlementLedgerEntry)
    fun ledgerForUser(userId: String): List<EntitlementLedgerEntry>
    fun saveRtdnEvent(event: RtdnEvent): RtdnEvent
    fun hasRtdnMessage(messageId: String): Boolean
}

class InMemoryPurchaseRepository : PurchaseRepository {
    private val purchasesById = linkedMapOf<UUID, PurchaseRecord>()
    private val purchaseIdsByTokenAndProduct = linkedMapOf<String, UUID>()
    private val ledgerEntries = mutableListOf<EntitlementLedgerEntry>()
    private val rtdnEvents = mutableListOf<RtdnEvent>()

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
    override fun purchaseByTokenHash(purchaseTokenHash: String): PurchaseRecord? {
        val id = purchaseIdsByTokenAndProduct.entries
            .firstOrNull { it.key.startsWith("$purchaseTokenHash:") }
            ?.value
        return id?.let { purchasesById[it] }
    }

    @Synchronized
    override fun updatePurchaseStateByTokenHash(purchaseTokenHash: String, state: PurchaseState): PurchaseRecord? {
        val existing = purchaseByTokenHash(purchaseTokenHash) ?: return null
        val updated = existing.copy(state = state)
        purchasesById[updated.id] = updated
        return updated
    }

    @Synchronized
    override fun hasGrantForPurchase(purchaseId: UUID): Boolean =
        ledgerEntries.any { it.purchaseId == purchaseId }

    @Synchronized
    override fun saveLedgerEntry(entry: EntitlementLedgerEntry) {
        ledgerEntries += entry
    }

    @Synchronized
    override fun ledgerForUser(userId: String): List<EntitlementLedgerEntry> =
        ledgerEntries.filter { it.userId == userId }

    @Synchronized
    override fun saveRtdnEvent(event: RtdnEvent): RtdnEvent {
        rtdnEvents += event
        return event
    }

    @Synchronized
    override fun hasRtdnMessage(messageId: String): Boolean =
        rtdnEvents.any { it.messageId == messageId }
}
