package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RichmanPurchaseManager {
    private final Context context;
    private final PurchaseUpdateListener listener;
    private BillingClient billingClient;
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private final List<ProductDetails> productDetailsList = new ArrayList<>();

    public RichmanPurchaseManager(Context context, PurchaseUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        initializeBillingClient();
    }

    private void initializeBillingClient() {
        PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        listener.onPurchaseSuccess(purchase);
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                listener.onPurchaseCanceled();
            } else {
                listener.onPurchaseFailed(billingResult, "Purchase failed: " + billingResult.getDebugMessage());
            }
        };

        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(pendingPurchasesParams)
                .enableAutoServiceReconnection()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    launchPurchaseFlowForProduct();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private void launchPurchaseFlowForProduct() {
        if (billingClient == null || !billingClient.isReady()) return;

        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder().setProductId("com.legendsoftware.richman.coins.50").setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId("com.legendsoftware.richman.coins.100").setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId("com.legendsoftware.richman.coins.200").setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId("com.legendsoftware.richman.coins.500").setProductType(BillingClient.ProductType.INAPP).build()
                )).build();

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

//                productDetailsList.clear();
//
//                for (ProductDetails productDetails : queryProductDetailsResult.getProductDetailsList()) {
//                    productDetailsMap.put(productDetails.getProductId(), productDetails);
//                    productDetailsList.add(productDetails);
//                }

                List<ProductDetails> sortedProducts = sortProductsByPriceSequence(queryProductDetailsResult.getProductDetailsList());
                productDetailsList.clear();
                productDetailsList.addAll(sortedProducts);

                productDetailsMap.clear();
                for (ProductDetails pd : sortedProducts) {
                    productDetailsMap.put(pd.getProductId(), pd);
                }

                if (listener != null) {
                    ((Activity) context).runOnUiThread(() -> listener.onProductsLoaded(new ArrayList<>(productDetailsList)));
                }
            }
        });
    }

    private List<ProductDetails> sortProductsByPriceSequence(List<ProductDetails> products) {
        List<ProductDetails> sortedList = new ArrayList<>(products);
        sortedList.sort((p1, p2) -> {
            long price1 = p1.getOneTimePurchaseOfferDetails() != null ? p1.getOneTimePurchaseOfferDetails().getPriceAmountMicros() : 0;
            long price2 = p2.getOneTimePurchaseOfferDetails() != null ? p2.getOneTimePurchaseOfferDetails().getPriceAmountMicros() : 0;
            return Long.compare(price1, price2);
        });
        return sortedList;
    }
    public void launchPurchase(String productId) {
        if (billingClient == null || !billingClient.isReady()) return;
        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) return;

        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build();

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(ImmutableList.of(params))
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }
    public void consumePurchase(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Handle the success of the consume operation.
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    //    private List<ProductDetails> sortProductsByPriceSequence(List<ProductDetails> products) {
    //        Map<String, ProductDetails> map = new HashMap<>();
    //        for (ProductDetails pd : products) {
    //            map.put(pd.getProductId(), pd);
    //        }
    //
    //        List<String> items = List.of(
    //                "com.legendsoftware.richman.coins.50",
    //                "com.legendsoftware.richman.coins.100",
    //                "com.legendsoftware.richman.coins.200",
    //                "com.legendsoftware.richman.coins.500"
    //        );
    //
    //        List<ProductDetails> sortedList = new ArrayList<>();
    //        for (String id : items) {
    //            if (map.containsKey(id)) sortedList.add(map.get(id));
    //        }
    //
    //        return sortedList;
    //    }

}
