package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

class RichmanPurchaseManager(
    private val context: Context,
    private val listener: PurchaseUpdateListener,
) {
    private val productDetailsStore = ProductDetailsStore()
    private lateinit var billingClient: BillingClient
    private lateinit var purchaseFlowLauncher: PurchaseFlowLauncher

    init {
        initializeBillingClient()
    }

    fun launchPurchase(productId: String) {
        purchaseFlowLauncher.launchPurchase(productId)
    }

    fun launchStarterMultiProductBundle() {
        purchaseFlowLauncher.launchStarterMultiProductBundle()
    }

    fun launchPremiumSubscriptionAddOns() {
        purchaseFlowLauncher.launchPremiumSubscriptionAddOns()
    }

    fun consumePurchase(purchase: Purchase?) {
        purchaseFlowLauncher.consumePurchase(purchase)
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                when {
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                        purchases
                            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                            .forEach(listener::onPurchaseSuccess)
                    }
                    billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                        listener.onPurchaseCanceled()
                    }
                    else -> {
                        listener.onPurchaseFailed(
                            billingResult,
                            "Purchase failed: ${billingResult.debugMessage}",
                        )
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        purchaseFlowLauncher = PurchaseFlowLauncher(context, billingClient, productDetailsStore)

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    ProductDetailsLoader(
                        billingClient = billingClient,
                        onLoaded = ::onProductsLoaded,
                        onFailed = ::notifyPurchaseFailed,
                    ).loadAllProducts()
                } else {
                    notifyPurchaseFailed(
                        billingResult,
                        "Billing unavailable: ${billingResult.debugMessage}",
                    )
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun onProductsLoaded(products: List<ProductDetails>) {
        productDetailsStore.replaceAll(products)
        (context as Activity).runOnUiThread {
            listener.onProductsLoaded(productDetailsStore.all())
        }
    }

    private fun notifyPurchaseFailed(billingResult: BillingResult, message: String) {
        (context as Activity).runOnUiThread {
            listener.onPurchaseFailed(billingResult, message)
        }
    }

    companion object {
        const val COINS_50_PRODUCT_ID = PurchaseProducts.COINS_50_PRODUCT_ID
        const val COINS_100_PRODUCT_ID = PurchaseProducts.COINS_100_PRODUCT_ID
        const val COINS_200_PRODUCT_ID = PurchaseProducts.COINS_200_PRODUCT_ID
        const val COINS_500_PRODUCT_ID = PurchaseProducts.COINS_500_PRODUCT_ID
        const val PREMIUM_MONTHLY_SUBSCRIPTION_ID = PurchaseProducts.PREMIUM_MONTHLY_SUBSCRIPTION_ID
        const val PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID = PurchaseProducts.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID
        const val PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID = PurchaseProducts.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID
        const val PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID = PurchaseProducts.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
        const val STARTER_BUNDLE_PRODUCT_ID = PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID
    }
}
