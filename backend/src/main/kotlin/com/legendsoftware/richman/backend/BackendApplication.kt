package com.legendsoftware.richman.backend

import com.legendsoftware.richman.backend.api.PurchaseSyncServer
import com.legendsoftware.richman.backend.play.EnvironmentPlayPurchaseVerifier
import com.legendsoftware.richman.backend.store.InMemoryPurchaseRepository

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val repository = InMemoryPurchaseRepository()
    val service = RichmanBackendFactory.create(
        repository = repository,
        verifier = EnvironmentPlayPurchaseVerifier(),
    )
    val server = PurchaseSyncServer(
        port = port,
        purchaseSyncService = service.purchaseSyncService,
        entitlementService = service.entitlementService,
    )

    server.start()
    println("Richman backend listening on http://localhost:$port")
}
