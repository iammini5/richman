package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.List;

final class ProductDetailsLoader {
    interface Callback {
        void onProductsLoaded(List<ProductDetails> products);

        void onProductsLoadFailed(BillingResult billingResult, String message);
    }

    private final BillingClient billingClient;
    private final Callback callback;

    ProductDetailsLoader(BillingClient billingClient, Callback callback) {
        this.billingClient = billingClient;
        this.callback = callback;
    }

    void loadAllProducts() {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }

        QueryProductDetailsParams oneTimeParams = QueryProductDetailsParams.newBuilder()
                .setProductList(PurchaseProducts.oneTimeProductQueries())
                .build();

        billingClient.queryProductDetailsAsync(oneTimeParams, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                loadSubscriptionProducts(new ArrayList<>(queryProductDetailsResult.getProductDetailsList()));
            } else {
                callback.onProductsLoadFailed(
                        billingResult,
                        "Unable to load coin products: " + billingResult.getDebugMessage()
                );
            }
        });
    }

    private void loadSubscriptionProducts(List<ProductDetails> products) {
        QueryProductDetailsParams subscriptionParams = QueryProductDetailsParams.newBuilder()
                .setProductList(PurchaseProducts.subscriptionProductQueries())
                .build();

        billingClient.queryProductDetailsAsync(subscriptionParams, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                products.addAll(queryProductDetailsResult.getProductDetailsList());
            }
            callback.onProductsLoaded(products);
        });
    }
}
