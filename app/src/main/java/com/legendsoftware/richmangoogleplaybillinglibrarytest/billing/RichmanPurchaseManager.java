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
    public static final String COINS_50_PRODUCT_ID = "com.legendsoftware.richman.coins.50";
    public static final String COINS_100_PRODUCT_ID = "com.legendsoftware.richman.coins.100";
    public static final String COINS_200_PRODUCT_ID = "com.legendsoftware.richman.coins.200";
    public static final String COINS_500_PRODUCT_ID = "com.legendsoftware.richman.coins.500";
    public static final String PREMIUM_MONTHLY_SUBSCRIPTION_ID = "premium_monthly";
    public static final String PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID = "premium_basic_monthly";
    public static final String PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID = "premium_plus_monthly";
    public static final String PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID = "premium_pro_monthly";
    public static final String STARTER_BUNDLE_PRODUCT_ID = "com.legendsoftware.richman.bundle.starter";

    private static final List<String> STARTER_MULTI_PRODUCT_BUNDLE_IDS = List.of(
            STARTER_BUNDLE_PRODUCT_ID,
            COINS_100_PRODUCT_ID,
            COINS_200_PRODUCT_ID
    );

    private static final List<String> PREMIUM_SUBSCRIPTION_ADD_ON_IDS = List.of(
            PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID,
            PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID,
            PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
    );

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
                } else if (listener != null) {
                    notifyPurchaseFailed(billingResult, "Billing unavailable: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private void launchPurchaseFlowForProduct() {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }

        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder().setProductId(COINS_50_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId(COINS_100_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId(COINS_200_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId(COINS_500_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
                        QueryProductDetailsParams.Product.newBuilder().setProductId(STARTER_BUNDLE_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
                )).build();

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                loadSubscriptionProducts(new ArrayList<>(queryProductDetailsResult.getProductDetailsList()));
            } else {
                notifyPurchaseFailed(billingResult, "Unable to load coin products: " + billingResult.getDebugMessage());
            }
        });
    }

    private void loadSubscriptionProducts(List<ProductDetails> products) {
        QueryProductDetailsParams subscriptionQueryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PREMIUM_MONTHLY_SUBSCRIPTION_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                ))
                .build();

        billingClient.queryProductDetailsAsync(subscriptionQueryParams, (billingResult, queryProductDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                products.addAll(queryProductDetailsResult.getProductDetailsList());
            }
            updateLoadedProducts(products);
        });
    }

    private void updateLoadedProducts(List<ProductDetails> products) {
        List<ProductDetails> sortedProducts = sortProductsByPriceSequence(deduplicateProducts(products));
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

    private void notifyPurchaseFailed(BillingResult billingResult, String message) {
        if (listener == null) {
            return;
        }
        ((Activity) context).runOnUiThread(() -> listener.onPurchaseFailed(billingResult, message));
    }

    private List<ProductDetails> sortProductsByPriceSequence(List<ProductDetails> products) {
        List<ProductDetails> sortedList = new ArrayList<>(products);
        sortedList.sort((p1, p2) -> {
            long price1 = getProductSortPrice(p1);
            long price2 = getProductSortPrice(p2);
            return Long.compare(price1, price2);
        });
        return sortedList;
    }

    private long getProductSortPrice(ProductDetails productDetails) {
        ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer = getDefaultOneTimePurchaseOffer(productDetails);
        if (oneTimeOffer != null) {
            return oneTimeOffer.getPriceAmountMicros();
        }
        if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            List<ProductDetails.PricingPhase> pricingPhases = productDetails.getSubscriptionOfferDetails()
                    .get(0)
                    .getPricingPhases()
                    .getPricingPhaseList();
            if (!pricingPhases.isEmpty()) {
                return pricingPhases.get(0).getPriceAmountMicros();
            }
        }
        return Long.MAX_VALUE;
    }

    private List<ProductDetails> deduplicateProducts(List<ProductDetails> products) {
        Map<String, ProductDetails> uniqueProducts = new HashMap<>();
        for (ProductDetails product : products) {
            uniqueProducts.putIfAbsent(product.getProductId(), product);
        }
        return new ArrayList<>(uniqueProducts.values());
    }

    public void launchPurchase(String productId) {
        if (billingClient == null || !billingClient.isReady()) return;
        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) return;

        BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
        if (params == null) return;

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(ImmutableList.of(params))
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }

    public void launchStarterMultiProductBundle() {
        launchMultiProductPurchase(STARTER_MULTI_PRODUCT_BUNDLE_IDS);
    }

    public void launchPremiumSubscriptionAddOns() {
        launchSubscriptionAddOns(PREMIUM_SUBSCRIPTION_ADD_ON_IDS);
    }

    public void launchMultiProductPurchase(List<String> productIds) {
        if (billingClient == null || !billingClient.isReady()) return;

        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<>();
        for (String productId : productIds) {
            ProductDetails productDetails = productDetailsMap.get(productId);
            if (productDetails == null || productDetails.getSubscriptionOfferDetails() != null) {
                continue;
            }

            BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
            if (params != null) {
                paramsList.add(params);
            }
        }

        if (paramsList.isEmpty()) return;

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(paramsList)
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }

    public void launchSubscriptionAddOns(List<String> productIds) {
        if (billingClient == null || !billingClient.isReady()) return;

        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<>();
        for (String productId : productIds) {
            ProductDetails productDetails = productDetailsMap.get(productId);
            if (productDetails == null || productDetails.getSubscriptionOfferDetails() == null) {
                continue;
            }

            BillingFlowParams.ProductDetailsParams params = buildProductDetailsParams(productDetails);
            if (params != null) {
                paramsList.add(params);
            }
        }

        if (paramsList.isEmpty()) return;

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(paramsList)
                .build();

        billingClient.launchBillingFlow((Activity) context, billingFlowParams);
    }

    private BillingFlowParams.ProductDetailsParams buildProductDetailsParams(ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams.Builder paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            paramsBuilder.setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken());
        }

        ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer = getDefaultOneTimePurchaseOffer(productDetails);
        if (oneTimeOffer != null) {
            paramsBuilder.setOfferToken(oneTimeOffer.getOfferToken());
        }

        return paramsBuilder.build();
    }

    public ProductDetails.OneTimePurchaseOfferDetails getDefaultOneTimePurchaseOffer(ProductDetails productDetails) {
        if (productDetails.getOneTimePurchaseOfferDetailsList() == null
                || productDetails.getOneTimePurchaseOfferDetailsList().isEmpty()) {
            return null;
        }
        return productDetails.getOneTimePurchaseOfferDetailsList().get(0);
    }

    public void consumePurchase(Purchase purchase) {
        if (purchase == null || isSubscriptionPurchase(purchase)) {
            return;
        }

        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Handle the success of the consume operation.
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    private boolean isSubscriptionPurchase(Purchase purchase) {
        return purchase.getProducts().contains(PREMIUM_MONTHLY_SUBSCRIPTION_ID)
                || purchase.getProducts().contains(PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID)
                || purchase.getProducts().contains(PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID)
                || purchase.getProducts().contains(PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID);
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
