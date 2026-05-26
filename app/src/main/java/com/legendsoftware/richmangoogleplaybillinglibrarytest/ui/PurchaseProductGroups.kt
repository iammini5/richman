package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import com.android.billingclient.api.ProductDetails
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts

object PurchaseProductGroups {
    const val EXTRA_PRODUCT_GROUP = "extra_product_group"
    const val COINS = "coins"
    const val PREMIUM = "premium"
    const val BUNDLE = "bundle"

    fun productsForGroup(
        productDetailsList: List<ProductDetails?>?,
        productGroup: String,
    ): List<ProductDetails> {
        return productDetailsList
            ?.filterNotNull()
            ?.distinctBy { it.productId }
            ?.filter { productDetails -> productDetails.belongsTo(productGroup) }
            .orEmpty()
    }

    fun canShowPremiumSubscriptionBundle(productDetailsList: List<ProductDetails?>?, productGroup: String): Boolean {
        if (productGroup != BUNDLE) return false
        val productIds = productDetailsList
            ?.filterNotNull()
            ?.filter { it.isSubscription() }
            ?.map { it.productId }
            ?.toSet()
            .orEmpty()
        return productIds.contains(PurchaseProducts.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID) &&
            productIds.contains(PurchaseProducts.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID) &&
            productIds.contains(PurchaseProducts.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID)
    }

    fun isSubscriptionProductId(productId: String): Boolean =
        PurchaseProducts.isSubscriptionProductId(productId)

    private fun ProductDetails.belongsTo(productGroup: String): Boolean {
        return when (productGroup) {
            PREMIUM -> isSubscription()
            BUNDLE -> productId == PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID
            else -> !isSubscription() && productId != PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID
        }
    }

    private fun ProductDetails.isSubscription(): Boolean =
        !subscriptionOfferDetails.isNullOrEmpty()
}
