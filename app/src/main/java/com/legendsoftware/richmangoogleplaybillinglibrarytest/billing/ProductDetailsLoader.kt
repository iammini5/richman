package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import android.util.Log
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
        querySubscriptionProducts(PurchaseProducts.subscriptionProductQueries()) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(
                    TAG,
                    "Fetched subscription products: ${
                        queryProductDetailsResult.productDetailsList.joinToString { productDetails ->
                            "${productDetails.productId}[offers=${productDetails.subscriptionOfferDetails.orEmpty().joinToString { offer -> offer.basePlanId }}]"
                        }
                    }"
                )
                queryProductDetailsResult.unfetchedProductList.forEach { unfetchedProduct ->
                    Log.d(
                        TAG,
                        "Unfetched subscription product: ${unfetchedProduct.productId}, status=${unfetchedProduct.statusCode}",
                    )
                }
                products += queryProductDetailsResult.productDetailsList
                retryUnfetchedSubscriptionProducts(
                    productIds = queryProductDetailsResult.unfetchedProductList.map { it.productId },
                    products = products,
                )
            } else {
                Log.d(TAG, "Subscription product query failed: ${billingResult.debugMessage}")
                onLoaded(products)
            }
        }
    }

    private fun retryUnfetchedSubscriptionProducts(
        productIds: List<String>,
        products: MutableList<ProductDetails>,
    ) {
        val nextProductId = productIds.firstOrNull()
        if (nextProductId == null) {
            onLoaded(products)
            return
        }

        val query = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(nextProductId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        querySubscriptionProducts(listOf(query)) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val fetchedProducts = queryProductDetailsResult.productDetailsList
                if (fetchedProducts.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "Fetched subscription product on retry: ${
                            fetchedProducts.joinToString { productDetails ->
                                "${productDetails.productId}[offers=${productDetails.subscriptionOfferDetails.orEmpty().joinToString { offer -> offer.basePlanId }}]"
                            }
                        }"
                    )
                    products += fetchedProducts
                }
                queryProductDetailsResult.unfetchedProductList.forEach { unfetchedProduct ->
                    Log.d(
                        TAG,
                        "Still unfetched subscription product: ${unfetchedProduct.productId}, status=${unfetchedProduct.statusCode}",
                    )
                }
            } else {
                Log.d(TAG, "Subscription product retry failed for $nextProductId: ${billingResult.debugMessage}")
            }
            retryUnfetchedSubscriptionProducts(productIds.drop(1), products)
        }
    }

    private fun querySubscriptionProducts(
        queries: List<QueryProductDetailsParams.Product>,
        onResult: (BillingResult, com.android.billingclient.api.QueryProductDetailsResult) -> Unit,
    ) {
        val subscriptionParams = QueryProductDetailsParams.newBuilder()
            .setProductList(queries)
            .build()

        billingClient.queryProductDetailsAsync(subscriptionParams, onResult)
    }

    companion object {
        private const val TAG = "ProductDetailsLoader"
    }
}
