package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

public final class PurchaseProducts {
    public static final String COINS_50_PRODUCT_ID = "com.legendsoftware.richman.coins.50";
    public static final String COINS_100_PRODUCT_ID = "com.legendsoftware.richman.coins.100";
    public static final String COINS_200_PRODUCT_ID = "com.legendsoftware.richman.coins.200";
    public static final String COINS_500_PRODUCT_ID = "com.legendsoftware.richman.coins.500";
    public static final String PREMIUM_MONTHLY_SUBSCRIPTION_ID = "premium_monthly";
    public static final String PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID = "premium_basic_monthly";
    public static final String PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID = "premium_plus_monthly";
    public static final String PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID = "premium_pro_monthly";
    public static final String STARTER_BUNDLE_PRODUCT_ID = "com.legendsoftware.richman.bundle.starter";

    public static final List<String> STARTER_MULTI_PRODUCT_BUNDLE_IDS = List.of(
            STARTER_BUNDLE_PRODUCT_ID,
            COINS_100_PRODUCT_ID,
            COINS_200_PRODUCT_ID
    );

    public static final List<String> PREMIUM_SUBSCRIPTION_ADD_ON_IDS = List.of(
            PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID,
            PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID,
            PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
    );

    private PurchaseProducts() {
    }

    public static ImmutableList<QueryProductDetailsParams.Product> oneTimeProductQueries() {
        return ImmutableList.of(
                oneTimeProduct(COINS_50_PRODUCT_ID),
                oneTimeProduct(COINS_100_PRODUCT_ID),
                oneTimeProduct(COINS_200_PRODUCT_ID),
                oneTimeProduct(COINS_500_PRODUCT_ID),
                oneTimeProduct(STARTER_BUNDLE_PRODUCT_ID)
        );
    }

    public static ImmutableList<QueryProductDetailsParams.Product> subscriptionProductQueries() {
        return ImmutableList.of(
                subscriptionProduct(PREMIUM_MONTHLY_SUBSCRIPTION_ID),
                subscriptionProduct(PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID),
                subscriptionProduct(PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID),
                subscriptionProduct(PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID)
        );
    }

    public static boolean isSubscriptionProductId(String productId) {
        return PREMIUM_MONTHLY_SUBSCRIPTION_ID.equals(productId) ||
                PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID.equals(productId) ||
                PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID.equals(productId) ||
                PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID.equals(productId);
    }

    private static QueryProductDetailsParams.Product oneTimeProduct(String productId) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
    }

    private static QueryProductDetailsParams.Product subscriptionProduct(String productId) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
    }
}
