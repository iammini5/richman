package com.legendsoftware.richman.backend

import com.legendsoftware.richman.backend.catalog.ProductCatalog
import com.legendsoftware.richman.backend.play.PlayPurchaseVerifier
import com.legendsoftware.richman.backend.service.EntitlementService
import com.legendsoftware.richman.backend.service.PurchaseSyncService
import com.legendsoftware.richman.backend.service.RtdnService
import com.legendsoftware.richman.backend.store.PurchaseRepository

object RichmanBackendFactory {
    fun create(
        repository: PurchaseRepository,
        verifier: PlayPurchaseVerifier,
        catalog: ProductCatalog = ProductCatalog.default(),
    ): Services {
        val entitlementService = EntitlementService(repository, catalog)
        val purchaseSyncService = PurchaseSyncService(
            repository = repository,
            verifier = verifier,
            catalog = catalog,
            entitlementService = entitlementService,
        )
        val rtdnService = RtdnService(
            repository = repository,
            purchaseSyncService = purchaseSyncService,
        )
        return Services(
            purchaseSyncService = purchaseSyncService,
            entitlementService = entitlementService,
            rtdnService = rtdnService,
        )
    }
}

data class Services(
    val purchaseSyncService: PurchaseSyncService,
    val entitlementService: EntitlementService,
    val rtdnService: RtdnService,
)
