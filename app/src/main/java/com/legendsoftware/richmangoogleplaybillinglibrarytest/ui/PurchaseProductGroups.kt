package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseOption

object PurchaseProductGroups {
    const val EXTRA_PRODUCT_GROUP = "extra_product_group"
    const val COINS = "coins"
    const val PREMIUM = "premium"
    const val BUNDLE = "bundle"

    val MORE_MENU_BUNDLE_ONE_TIME_PRODUCT_IDS = setOf(
        PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID,
    )

    val MORE_MENU_BUNDLE_SUBSCRIPTION_PRODUCT_IDS =
        PurchaseProducts.PREMIUM_SUBSCRIPTION_ADD_ON_IDS.toSet()

    fun productsForGroup(
        productOptions: List<PurchaseOption>,
        productGroup: String,
    ): List<PurchaseOption> {
        return productOptions
            .distinctBy { option -> "${option.productId}:${option.basePlanId}:${option.offerToken}" }
            .filter { option -> option.belongsTo(productGroup) }
    }

    fun canShowPremiumSubscriptionBundle(productOptions: List<PurchaseOption>, productGroup: String): Boolean {
        if (productGroup != BUNDLE) return false
        val productIds = productOptions
            .filter { it.isSubscription }
            .map { it.productId }
            .toSet()
        return productIds.containsAll(MORE_MENU_BUNDLE_SUBSCRIPTION_PRODUCT_IDS)
    }

    fun isSubscriptionProductId(productId: String): Boolean =
        PurchaseProducts.isSubscriptionProductId(productId)

    fun isMoreMenuBundleProductId(productId: String): Boolean =
        productId in MORE_MENU_BUNDLE_ONE_TIME_PRODUCT_IDS ||
            productId in MORE_MENU_BUNDLE_SUBSCRIPTION_PRODUCT_IDS

    private fun PurchaseOption.belongsTo(productGroup: String): Boolean {
        return when (productGroup) {
            PREMIUM -> isSubscription && productId !in MORE_MENU_BUNDLE_SUBSCRIPTION_PRODUCT_IDS
            BUNDLE -> productId in MORE_MENU_BUNDLE_ONE_TIME_PRODUCT_IDS
            else -> !isSubscription && productId !in MORE_MENU_BUNDLE_ONE_TIME_PRODUCT_IDS
        }
    }
}
