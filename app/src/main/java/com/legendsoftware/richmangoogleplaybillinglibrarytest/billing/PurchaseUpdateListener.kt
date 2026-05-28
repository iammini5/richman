package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

interface PurchaseUpdateListener {
    fun onProductsLoaded(productOptions: List<PurchaseOption>)
    fun onPurchaseSuccess(purchase: Purchase?)
    fun onPurchaseFailed(billingResult: BillingResult?, errorMessage: String?)
    fun onPurchasePending(purchase: Purchase?)
    fun onPurchaseCanceled()
    fun onSubscriptionStatusUpdated(purchaseToken: String?) = Unit
}
