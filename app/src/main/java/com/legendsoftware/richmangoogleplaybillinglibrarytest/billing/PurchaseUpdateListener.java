package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import java.util.List;

public interface PurchaseUpdateListener {

    void onProductsLoaded(List<ProductDetails> productDetailsList);

    void onPurchaseSuccess(Purchase purchase);

    void onPurchaseFailed(BillingResult billingResult, String errorMessage);

    void onPurchasePending(Purchase purchase);

    void onPurchaseCanceled();
}
