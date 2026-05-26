package com.legendsoftware.richmangoogleplaybillinglibrarytest.billing;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProductDetailsStore {
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private final List<ProductDetails> productDetailsList = new ArrayList<>();

    void replaceAll(List<ProductDetails> products) {
        List<ProductDetails> sortedProducts = sortProductsByPriceSequence(deduplicateProducts(products));
        productDetailsList.clear();
        productDetailsList.addAll(sortedProducts);

        productDetailsMap.clear();
        for (ProductDetails productDetails : sortedProducts) {
            productDetailsMap.put(productDetails.getProductId(), productDetails);
        }
    }

    ProductDetails get(String productId) {
        return productDetailsMap.get(productId);
    }

    List<ProductDetails> all() {
        return new ArrayList<>(productDetailsList);
    }

    static ProductDetails.OneTimePurchaseOfferDetails defaultOneTimeOffer(ProductDetails productDetails) {
        if (productDetails.getOneTimePurchaseOfferDetailsList() == null ||
                productDetails.getOneTimePurchaseOfferDetailsList().isEmpty()) {
            return null;
        }
        return productDetails.getOneTimePurchaseOfferDetailsList().get(0);
    }

    private List<ProductDetails> sortProductsByPriceSequence(List<ProductDetails> products) {
        List<ProductDetails> sortedList = new ArrayList<>(products);
        sortedList.sort((p1, p2) -> Long.compare(productSortPrice(p1), productSortPrice(p2)));
        return sortedList;
    }

    private long productSortPrice(ProductDetails productDetails) {
        ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer = defaultOneTimeOffer(productDetails);
        if (oneTimeOffer != null) {
            return oneTimeOffer.getPriceAmountMicros();
        }
        if (productDetails.getSubscriptionOfferDetails() != null &&
                !productDetails.getSubscriptionOfferDetails().isEmpty()) {
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
}
