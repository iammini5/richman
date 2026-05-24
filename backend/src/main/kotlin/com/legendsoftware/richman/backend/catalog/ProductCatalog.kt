package com.legendsoftware.richman.backend.catalog

data class ProductCatalog(
    private val products: Map<String, ProductRule>,
) {
    fun ruleFor(productId: String): ProductRule? = products[productId]

    fun requireRule(productId: String): ProductRule =
        ruleFor(productId) ?: error("Unsupported product: $productId")

    companion object {
        fun default(): ProductCatalog = ProductCatalog(
            mapOf(
                "com.legendsoftware.richman.coins.50" to ProductRule.OneTimeCoins(50),
                "com.legendsoftware.richman.coins.100" to ProductRule.OneTimeCoins(100),
                "com.legendsoftware.richman.coins.200" to ProductRule.OneTimeCoins(200),
                "com.legendsoftware.richman.coins.500" to ProductRule.OneTimeCoins(500),
                "com.legendsoftware.richman.bundle.starter" to ProductRule.StarterBundle(
                    coinAmount = 100,
                    entitlementKey = "starter_bundle",
                ),
                "premium_monthly" to ProductRule.Subscription("legacy", priority = 1),
                "premium_basic_monthly" to ProductRule.Subscription("basic", priority = 2),
                "premium_plus_monthly" to ProductRule.Subscription("plus", priority = 3),
                "premium_pro_monthly" to ProductRule.Subscription("pro", priority = 4),
            )
        )
    }
}

sealed interface ProductRule {
    data class OneTimeCoins(val coinAmount: Int) : ProductRule
    data class StarterBundle(val coinAmount: Int, val entitlementKey: String) : ProductRule
    data class Subscription(val tier: String, val priority: Int) : ProductRule
}
