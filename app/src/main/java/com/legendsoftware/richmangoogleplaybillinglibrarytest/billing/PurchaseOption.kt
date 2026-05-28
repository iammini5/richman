package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.ProductDetails

data class PurchaseOption(
    val productDetails: ProductDetails,
    val subscriptionOffer: ProductDetails.SubscriptionOfferDetails? = null,
) {
    val productId: String = productDetails.productId
    val basePlanId: String? = subscriptionOffer?.basePlanId
    val offerToken: String? = subscriptionOffer?.offerToken
    val isSubscription: Boolean = subscriptionOffer != null

    val isMonthlyBasePlan: Boolean =
        basePlanId?.endsWith("-monthly") == true || basePlanId == PurchaseProducts.MONTHLY_BASE_PLAN_ID

    val isYearlyBasePlan: Boolean =
        basePlanId?.endsWith("-yearly") == true || basePlanId == PurchaseProducts.YEARLY_BASE_PLAN_ID
}
