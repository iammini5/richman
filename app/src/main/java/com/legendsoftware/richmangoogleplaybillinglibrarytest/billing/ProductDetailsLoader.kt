package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams

internal class ProductDetailsLoader(
    private val billingClient: BillingClient,
    private val onLoaded: (List<ProductDetails>) -> Unit,
    private val onFailed: (BillingResult, String) -> Unit,
) {
    fun loadAllProducts() {
        if (!billingClient.isReady) return

        val oneTimeParams = QueryProductDetailsParams.newBuilder()
            .setProductList(PurchaseProducts.oneTimeProductQueries())
            .build()

        billingClient.queryProductDetailsAsync(oneTimeParams) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                loadSubscriptionProducts(queryProductDetailsResult.productDetailsList.toMutableList())
            } else {
                onFailed(
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
            onLoaded(products)
        }
    }
}
