package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.QueryProductDetailsParams

object PurchaseProducts {
    const val COINS_50_PRODUCT_ID = "com.legendsoftware.richman.coins.50"
    const val COINS_100_PRODUCT_ID = "com.legendsoftware.richman.coins.100"
    const val COINS_200_PRODUCT_ID = "com.legendsoftware.richman.coins.200"
    const val COINS_500_PRODUCT_ID = "com.legendsoftware.richman.coins.500"
    const val PREMIUM_MONTHLY_SUBSCRIPTION_ID = "premium_monthly"
    const val PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID = "premium_basic_monthly"
    const val PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID = "premium_plus_monthly"
    const val PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID = "premium_pro_monthly"
    const val STARTER_BUNDLE_PRODUCT_ID = "com.legendsoftware.richman.bundle.starter"

    val STARTER_MULTI_PRODUCT_BUNDLE_IDS = listOf(
        STARTER_BUNDLE_PRODUCT_ID,
        COINS_100_PRODUCT_ID,
        COINS_200_PRODUCT_ID,
    )

    val PREMIUM_SUBSCRIPTION_ADD_ON_IDS = listOf(
        PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID,
        PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID,
        PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID,
    )

    fun oneTimeProductQueries(): List<QueryProductDetailsParams.Product> = listOf(
        oneTimeProduct(COINS_50_PRODUCT_ID),
        oneTimeProduct(COINS_100_PRODUCT_ID),
        oneTimeProduct(COINS_200_PRODUCT_ID),
        oneTimeProduct(COINS_500_PRODUCT_ID),
        oneTimeProduct(STARTER_BUNDLE_PRODUCT_ID),
    )

    fun subscriptionProductQueries(): List<QueryProductDetailsParams.Product> = listOf(
        subscriptionProduct(PREMIUM_MONTHLY_SUBSCRIPTION_ID),
        subscriptionProduct(PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID),
        subscriptionProduct(PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID),
        subscriptionProduct(PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID),
    )

    fun isSubscriptionProductId(productId: String): Boolean =
        productId == PREMIUM_MONTHLY_SUBSCRIPTION_ID ||
            productId == PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID ||
            productId == PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID ||
            productId == PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID

    private fun oneTimeProduct(productId: String): QueryProductDetailsParams.Product =
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

    private fun subscriptionProduct(productId: String): QueryProductDetailsParams.Product =
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
}
