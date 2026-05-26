package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

final class PurchaseFlowLauncher {
    private final Context context;
    private final BillingClient billingClient;
    private final ProductDetailsStore productDetailsStore;

    PurchaseFlowLauncher(
            Context context,
            BillingClient billingClient,
            ProductDetailsStore productDetailsStore
    ) {
        this.context = context;
        this.billingClient = billingClient;
        this.productDetailsStore = productDetailsStore;
    }

    void launchPurchase(String productId) {
        if (billingClient == null || !billingClient.isReady()) return;
        ProductDetails productDetails = productDetailsStore.get(productId);
        if (productDetails == null) return;

        BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
        if (params == null) return;

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(ImmutableList.of(params))
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }

    void launchStarterMultiProductBundle() {
        launchMultiProductPurchase(PurchaseProducts.STARTER_MULTI_PRODUCT_BUNDLE_IDS);
    }

    void launchPremiumSubscriptionAddOns() {
        launchSubscriptionAddOns(PurchaseProducts.PREMIUM_SUBSCRIPTION_ADD_ON_IDS);
    }

    void consumePurchase(Purchase purchase) {
        if (purchase == null || isSubscriptionPurchase(purchase)) {
            return;
        }

        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            // Google Play owns the consumption result; local UI already fulfilled verified purchases.
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    private void launchMultiProductPurchase(List<String> productIds) {
        if (billingClient == null || !billingClient.isReady()) return;

        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<>();
        for (String productId : productIds) {
            ProductDetails productDetails = productDetailsStore.get(productId);
            if (productDetails == null || productDetails.getSubscriptionOfferDetails() != null) {
                continue;
            }

            BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
            if (params != null) {
                paramsList.add(params);
            }
        }

        launchMultiProductFlow(paramsList);
    }

    private void launchSubscriptionAddOns(List<String> productIds) {
        if (billingClient == null || !billingClient.isReady()) return;

        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<>();
        for (String productId : productIds) {
            ProductDetails productDetails = productDetailsStore.get(productId);
            if (productDetails == null || productDetails.getSubscriptionOfferDetails() == null) {
                continue;
            }

            BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
            if (params != null) {
                paramsList.add(params);
            }
        }

        launchMultiProductFlow(paramsList);
    }

    private void launchMultiProductFlow(List<BillingFlowParams.ProductDetailsParams> paramsList) {
        if (paramsList.isEmpty()) return;

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(paramsList)
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }

    private BillingFlowParams.ProductDetailsParams buildProductDetailsParams(ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams.Builder paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (productDetails.getSubscriptionOfferDetails() != null &&
                !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            paramsBuilder.setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken());
        }

        ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer =
                ProductDetailsStore.defaultOneTimeOffer(productDetails);
        if (oneTimeOffer != null) {
            paramsBuilder.setOfferToken(oneTimeOffer.getOfferToken());
        }

        return paramsBuilder.build();
    }

    private boolean isSubscriptionPurchase(Purchase purchase) {
        for (String productId : purchase.getProducts()) {
            if (PurchaseProducts.isSubscriptionProductId(productId)) {
                return true;
            }
        }
        return false;
    }
}
