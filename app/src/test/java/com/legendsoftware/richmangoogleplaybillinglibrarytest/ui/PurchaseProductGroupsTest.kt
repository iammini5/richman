package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseProductGroupsTest {
    @Test
    fun moreMenuBundleProductsIncludeStarterBundleCheckout() {
        assertEquals(
            setOf(PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID),
            PurchaseProductGroups.MORE_MENU_BUNDLE_ONE_TIME_PRODUCT_IDS,
        )
        assertTrue(
            PurchaseProductGroups.isMoreMenuBundleProductId(
                PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID,
            ),
        )
    }

    @Test
    fun moreMenuBundleProductsIncludePremiumSubscriptionAddOnCheckout() {
        assertEquals(
            PurchaseProducts.PREMIUM_SUBSCRIPTION_ADD_ON_IDS.toSet(),
            PurchaseProductGroups.MORE_MENU_BUNDLE_SUBSCRIPTION_PRODUCT_IDS,
        )

        PurchaseProducts.PREMIUM_SUBSCRIPTION_ADD_ON_IDS.forEach { productId ->
            assertTrue(PurchaseProductGroups.isMoreMenuBundleProductId(productId))
        }
    }

    @Test
    fun standaloneCoinPacksAreNotBundleProducts() {
        assertFalse(PurchaseProductGroups.isMoreMenuBundleProductId(PurchaseProducts.COINS_50_PRODUCT_ID))
        assertFalse(PurchaseProductGroups.isMoreMenuBundleProductId(PurchaseProducts.COINS_100_PRODUCT_ID))
        assertFalse(PurchaseProductGroups.isMoreMenuBundleProductId(PurchaseProducts.COINS_200_PRODUCT_ID))
        assertFalse(PurchaseProductGroups.isMoreMenuBundleProductId(PurchaseProducts.COINS_500_PRODUCT_ID))
    }
}
