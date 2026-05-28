package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.ProductDetails

internal class ProductDetailsStore {
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    private val productDetailsList = mutableListOf<ProductDetails>()

    fun replaceAll(products: List<ProductDetails>) {
        val sortedProducts = products
            .distinctBy { it.productId }
            .sortedBy { it.productSortPrice() }

        productDetailsList.clear()
        productDetailsList.addAll(sortedProducts)

        productDetailsMap.clear()
        sortedProducts.associateByTo(productDetailsMap) { it.productId }
    }

    fun get(productId: String): ProductDetails? = productDetailsMap[productId]

    fun all(): List<ProductDetails> = productDetailsList.toList()

    fun optionsFor(products: List<ProductDetails>): List<PurchaseOption> =
        products.flatMap { productDetails ->
            val subscriptionOffers = productDetails.subscriptionOfferDetails.orEmpty()
            if (subscriptionOffers.isEmpty()) {
                listOf(PurchaseOption(productDetails))
            } else {
                subscriptionOffers
                    .sortedWith(
                        compareBy<ProductDetails.SubscriptionOfferDetails> { offer ->
                            when {
                                offer.basePlanId.endsWith("-monthly") || offer.basePlanId == PurchaseProducts.MONTHLY_BASE_PLAN_ID -> 0
                                offer.basePlanId.endsWith("-yearly") || offer.basePlanId == PurchaseProducts.YEARLY_BASE_PLAN_ID -> 1
                                else -> 2
                            }
                        }.thenBy { it.basePlanId }
                    )
                    .map { offer -> PurchaseOption(productDetails, offer) }
            }
        }

    companion object {
        fun defaultOneTimeOffer(productDetails: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? =
            productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()
    }

    private fun ProductDetails.productSortPrice(): Long {
        defaultOneTimeOffer(this)?.let { return it.priceAmountMicros }
        val subscriptionPrice = subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.priceAmountMicros
        return subscriptionPrice ?: Long.MAX_VALUE
    }
}
