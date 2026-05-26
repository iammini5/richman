package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams

internal class ProductDetailsLoader(
    private val billingClient: BillingClient,
    private val callback: Callback,
) {
    interface Callback {
        fun onProductsLoaded(products: List<ProductDetails>)
        fun onProductsLoadFailed(billingResult: BillingResult, message: String)
    }

    fun loadAllProducts() {
        if (!billingClient.isReady) return

        val oneTimeParams = QueryProductDetailsParams.newBuilder()
            .setProductList(PurchaseProducts.oneTimeProductQueries())
            .build()

        billingClient.queryProductDetailsAsync(oneTimeParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                loadSubscriptionProducts(queryProductDetailsResult.productDetailsList.toMutableList())
            } else {
                callback.onProductsLoadFailed(
                    billingResult,
                    "Unable to load coin products: ${billingResult.debugMessage}",
                )
            }
        }
    }

    private fun loadSubscriptionProducts(products: MutableList<ProductDetails>) {
        val subscriptionParams = QueryProductDetailsParams.newBuilder()
            .setProductList(PurchaseProducts.subscriptionProductQueries())
            .build()

        billingClient.queryProductDetailsAsync(subscriptionParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                products += queryProductDetailsResult.productDetailsList
            }
            callback.onProductsLoaded(products)
        }
    }
}
