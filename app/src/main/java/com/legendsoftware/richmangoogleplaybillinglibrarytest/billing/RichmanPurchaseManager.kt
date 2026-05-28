package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams

class RichmanPurchaseManager(
    private val context: Context,
    private val listener: PurchaseUpdateListener,
) {
    private val productDetailsStore = ProductDetailsStore()
    private lateinit var billingClient: BillingClient
    private lateinit var purchaseFlowLauncher: PurchaseFlowLauncher
    private var inAppMessageRequestSubmitted = false

    init {
        initializeBillingClient()
    }

    fun launchPurchase(option: PurchaseOption) {
        if (option.isMonthlyBasePlan && PurchaseProducts.isNewPremiumTierProductId(option.productId)) {
            queryActiveSubscriptionFor(option.productId) { oldPurchase ->
                purchaseFlowLauncher.launchPurchase(option, oldPurchase?.purchaseToken)
            }
        } else {
            purchaseFlowLauncher.launchPurchase(option)
        }
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

    fun showSubscriptionInAppMessages() {
        if (!billingClient.isReady || inAppMessageRequestSubmitted) return

        val featureResult = billingClient.isFeatureSupported(BillingClient.FeatureType.IN_APP_MESSAGING)
        if (featureResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing in-app messaging unavailable: ${featureResult.debugMessage}")
            return
        }

        val params = InAppMessageParams.newBuilder()
            .addInAppMessageCategoryToShow(InAppMessageParams.InAppMessageCategoryId.TRANSACTIONAL)
            .build()

        val requestResult = billingClient.showInAppMessages(context as Activity, params) { result ->
            when (result.responseCode) {
                InAppMessageResult.InAppMessageResponseCode.NO_ACTION_NEEDED -> {
                    Log.d(TAG, "No subscription in-app message needed")
                }
                InAppMessageResult.InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED -> {
                    Log.d(TAG, "Subscription status updated from in-app message")
                    listener.onSubscriptionStatusUpdated(result.purchaseToken)
                }
                else -> {
                    Log.d(TAG, "Subscription in-app message result: ${result.responseCode}")
                }
            }
        }

        if (requestResult.responseCode == BillingClient.BillingResponseCode.OK) {
            inAppMessageRequestSubmitted = true
        } else {
            Log.d(TAG, "Subscription in-app message request failed: ${requestResult.debugMessage}")
        }
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
            listener.onProductsLoaded(productDetailsStore.optionsFor(productDetailsStore.all()))
            showSubscriptionInAppMessages()
        }
    }

    private fun queryActiveSubscriptionFor(productId: String, onResult: (Purchase?) -> Unit) {
        if (!billingClient.isReady) {
            onResult(null)
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(purchases.firstOrNull { purchase -> productId in purchase.products })
            } else {
                Log.d(TAG, "Active subscription query failed: ${billingResult.debugMessage}")
                onResult(null)
            }
        }
    }

    private fun notifyPurchaseFailed(billingResult: BillingResult, message: String) {
        (context as Activity).runOnUiThread {
            listener.onPurchaseFailed(billingResult, message)
        }
    }

    companion object {
        private const val TAG = "RichmanPurchaseManager"
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
