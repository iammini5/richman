package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase

internal class PurchaseFlowLauncher(
    private val context: Context,
    private val billingClient: BillingClient,
    private val productDetailsStore: ProductDetailsStore,
) {
    fun launchPurchase(productId: String) {
        if (!billingClient.isReady) return
        val productDetails = productDetailsStore.get(productId) ?: return
        val params = productDetailsParams(productDetails)

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()

        billingClient.launchBillingFlow(context as Activity, billingFlowParams)
    }

    fun launchStarterMultiProductBundle() {
        launchMultiProductPurchase(PurchaseProducts.STARTER_MULTI_PRODUCT_BUNDLE_IDS)
    }

    fun launchPremiumSubscriptionAddOns() {
        launchSubscriptionAddOns(PurchaseProducts.PREMIUM_SUBSCRIPTION_ADD_ON_IDS)
    }

    fun consumePurchase(purchase: Purchase?) {
        if (purchase == null || purchase.products.any(PurchaseProducts::isSubscriptionProductId)) {
            return
        }

        val consumeParams = com.android.billingclient.api.ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { _, _ ->
            // Google Play owns the consumption result; local UI already fulfilled verified purchases.
        }
    }

    private fun launchMultiProductPurchase(productIds: List<String>) {
        if (!billingClient.isReady) return
        val params = productIds.mapNotNull { productId ->
            val productDetails = productDetailsStore.get(productId) ?: return@mapNotNull null
            if (!productDetails.subscriptionOfferDetails.isNullOrEmpty()) return@mapNotNull null
            productDetailsParams(productDetails)
        }
        launchMultiProductFlow(params)
    }

    private fun launchSubscriptionAddOns(productIds: List<String>) {
        if (!billingClient.isReady) return
        val params = productIds.mapNotNull { productId ->
            val productDetails = productDetailsStore.get(productId) ?: return@mapNotNull null
            if (productDetails.subscriptionOfferDetails.isNullOrEmpty()) return@mapNotNull null
            productDetailsParams(productDetails)
        }
        launchMultiProductFlow(params)
    }

    private fun launchMultiProductFlow(params: List<BillingFlowParams.ProductDetailsParams>) {
        if (params.isEmpty()) return

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(params)
            .build()

        billingClient.launchBillingFlow(context as Activity, billingFlowParams)
    }

    private fun productDetailsParams(
        productDetails: com.android.billingclient.api.ProductDetails,
    ): BillingFlowParams.ProductDetailsParams {
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?.let(paramsBuilder::setOfferToken)

        ProductDetailsStore.defaultOneTimeOffer(productDetails)
            ?.offerToken
            ?.let(paramsBuilder::setOfferToken)

        return paramsBuilder.build()
    }
}
