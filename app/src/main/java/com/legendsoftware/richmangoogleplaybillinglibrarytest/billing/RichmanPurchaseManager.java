package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class RichmanPurchaseManager implements ProductDetailsLoader.Callback {
    public static final String COINS_50_PRODUCT_ID = PurchaseProducts.COINS_50_PRODUCT_ID;
    public static final String COINS_100_PRODUCT_ID = PurchaseProducts.COINS_100_PRODUCT_ID;
    public static final String COINS_200_PRODUCT_ID = PurchaseProducts.COINS_200_PRODUCT_ID;
    public static final String COINS_500_PRODUCT_ID = PurchaseProducts.COINS_500_PRODUCT_ID;
    public static final String PREMIUM_MONTHLY_SUBSCRIPTION_ID = PurchaseProducts.PREMIUM_MONTHLY_SUBSCRIPTION_ID;
    public static final String PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID =
            PurchaseProducts.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID;
    public static final String PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID =
            PurchaseProducts.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID;
    public static final String PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID =
            PurchaseProducts.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID;
    public static final String STARTER_BUNDLE_PRODUCT_ID = PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID;

    private final Context context;
    private final PurchaseUpdateListener listener;
    private final ProductDetailsStore productDetailsStore = new ProductDetailsStore();
    private BillingClient billingClient;
    private PurchaseFlowLauncher purchaseFlowLauncher;

    public RichmanPurchaseManager(Context context, PurchaseUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        initializeBillingClient();
    }

    public void launchPurchase(String productId) {
        if (purchaseFlowLauncher != null) {
            purchaseFlowLauncher.launchPurchase(productId);
        }
    }

    public void launchStarterMultiProductBundle() {
        if (purchaseFlowLauncher != null) {
            purchaseFlowLauncher.launchStarterMultiProductBundle();
        }
    }

    public void launchPremiumSubscriptionAddOns() {
        if (purchaseFlowLauncher != null) {
            purchaseFlowLauncher.launchPremiumSubscriptionAddOns();
        }
    }

    public void consumePurchase(Purchase purchase) {
        if (purchaseFlowLauncher != null) {
            purchaseFlowLauncher.consumePurchase(purchase);
        }
    }

    @Override
    public void onProductsLoaded(List<ProductDetails> products) {
        productDetailsStore.replaceAll(products);
        if (listener != null) {
            ((Activity) context).runOnUiThread(() -> listener.onProductsLoaded(productDetailsStore.all()));
        }
    }

    @Override
    public void onProductsLoadFailed(BillingResult billingResult, String message) {
        notifyPurchaseFailed(billingResult, message);
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
        purchaseFlowLauncher = new PurchaseFlowLauncher(context, billingClient, productDetailsStore);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    new ProductDetailsLoader(billingClient, RichmanPurchaseManager.this).loadAllProducts();
                } else if (listener != null) {
                    notifyPurchaseFailed(billingResult, "Billing unavailable: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private void notifyPurchaseFailed(BillingResult billingResult, String message) {
        if (listener == null) {
            return;
        }
        ((Activity) context).runOnUiThread(() -> listener.onPurchaseFailed(billingResult, message));
    }
}
