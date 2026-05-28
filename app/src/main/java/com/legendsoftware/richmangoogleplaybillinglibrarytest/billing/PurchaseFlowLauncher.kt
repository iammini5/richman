package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.Purchase

internal class PurchaseFlowLauncher(
    private val context: Context,
    private val billingClient: BillingClient,
    private val productDetailsStore: ProductDetailsStore,
) {
    fun launchPurchase(option: PurchaseOption, oldPurchaseToken: String? = null) {
        if (!billingClient.isReady) return
        val params = productDetailsParams(option)

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))

        if (oldPurchaseToken != null && option.isMonthlyBasePlan) {
            billingFlowParamsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldPurchaseToken)
                    .setSubscriptionReplacementMode(ReplacementMode.WITHOUT_PRORATION)
                    .build()
            )
        }

        billingClient.launchBillingFlow(context as Activity, billingFlowParamsBuilder.build())
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
            productDetailsParams(PurchaseOption(productDetails))
        }
        launchMultiProductFlow(params)
    }

    private fun launchSubscriptionAddOns(productIds: List<String>) {
        if (!billingClient.isReady) return
        val params = productIds.mapNotNull { productId ->
            val productDetails = productDetailsStore.get(productId) ?: return@mapNotNull null
            if (productDetails.subscriptionOfferDetails.isNullOrEmpty()) return@mapNotNull null
            productDetailsParams(PurchaseOption(productDetails, productDetails.subscriptionOfferDetails?.firstOrNull()))
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
        option: PurchaseOption,
    ): BillingFlowParams.ProductDetailsParams {
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(option.productDetails)

        option.offerToken
            ?.let(paramsBuilder::setOfferToken)

        ProductDetailsStore.defaultOneTimeOffer(option.productDetails)
            ?.offerToken
            ?.let(paramsBuilder::setOfferToken)

        return paramsBuilder.build()
    }
}
