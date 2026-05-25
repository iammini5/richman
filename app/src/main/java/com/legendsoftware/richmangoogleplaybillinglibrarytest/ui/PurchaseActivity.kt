package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.legendsoftware.richmangoogleplaybillinglibrarytest.backend.PurchaseSyncClient
import com.legendsoftware.richmangoogleplaybillinglibrarytest.adapter.PurchaseProductAdapter
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseUpdateListener
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.COINS_100_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.COINS_200_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.COINS_500_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.COINS_50_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager.STARTER_BUNDLE_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.ActivityPurchaseBinding
import com.legendsoftware.richmangoogleplaybillinglibrarytest.viewmodel.CoinManagerViewModel

class PurchaseActivity : AppCompatActivity(), PurchaseUpdateListener {
    private lateinit var binding: ActivityPurchaseBinding
    private val coinManager: CoinManagerViewModel by viewModels()
    private lateinit var purchaseManager: RichmanPurchaseManager
    private val purchaseSyncClient = PurchaseSyncClient()
    private lateinit var adapter: PurchaseProductAdapter
    private val productGroup: String
        get() = intent.getStringExtra(EXTRA_PRODUCT_GROUP) ?: PRODUCT_GROUP_COINS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //========== main ==========//
        configureScreenForProductGroup()
        purchaseManager = RichmanPurchaseManager(this, this)

        adapter = PurchaseProductAdapter(emptyList()) { productDetails ->
            if (productGroup == PRODUCT_GROUP_BUNDLE && productDetails.productId == STARTER_BUNDLE_PRODUCT_ID) {
                purchaseManager.launchStarterMultiProductBundle()
            } else {
                purchaseManager.launchPurchase(productDetails.productId)
            }
        }

        binding.inAppProducts.layoutManager = LinearLayoutManager(this)
        binding.inAppProducts.adapter = adapter
        binding.btnSubscriptionAddOns.setOnClickListener {
            purchaseManager.launchPremiumSubscriptionAddOns()
        }

        coinManager.coins.observe(this) { coins ->
            binding.coins.text = coins.toString()
        }

    }

    override fun onProductsLoaded(productDetailsList: List<ProductDetails?>?) {
        val safeList = productDetailsList
            ?.filterNotNull()
            ?.distinctBy { it.productId }
            ?.filter { productDetails ->
                val isSubscription = !productDetails.subscriptionOfferDetails.isNullOrEmpty()
                when (productGroup) {
                    PRODUCT_GROUP_PREMIUM -> isSubscription
                    PRODUCT_GROUP_BUNDLE -> productDetails.productId == STARTER_BUNDLE_PRODUCT_ID
                    else -> !isSubscription && productDetails.productId != STARTER_BUNDLE_PRODUCT_ID
                }
            } ?: emptyList()
        val canShowPremiumBundle = productGroup == PRODUCT_GROUP_BUNDLE &&
            productDetailsList.hasPremiumSubscriptionAddOns()

        adapter.setProducts(safeList)
        binding.btnSubscriptionAddOns.visibility = if (canShowPremiumBundle) View.VISIBLE else View.GONE
        if (safeList.isEmpty() && !canShowPremiumBundle) {
            binding.purchaseStatus.visibility = View.VISIBLE
            binding.purchaseStatus.text = when (productGroup) {
                PRODUCT_GROUP_PREMIUM -> "Premium subscription is not available yet. Please install from Google Play with a tester account and try again later."
                PRODUCT_GROUP_BUNDLE -> "Bundle offers are not available yet. Please install from Google Play with a tester account and try again later."
                else -> "Coin products are not available yet. Please install from Google Play with a tester account and try again later."
            }
        } else {
            binding.purchaseStatus.visibility = View.GONE
        }
    }

//    override fun onProductsLoaded(productDetailsList: List<ProductDetails?>?) {
//        val safeList = productDetailsList?.filterNotNull() ?: emptyList()
//        val sortedList = purchaseManager.sortProductsByPriceSequence(safeList)
//        adapter.setProducts(sortedList)
//    }


    override fun onPurchaseSuccess(purchase: Purchase?) {
        if (purchase == null) {
            Toast.makeText(this, "Purchase missing receipt. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val result = purchaseSyncClient.syncPurchase(purchase)
            runOnUiThread {
                result.onSuccess {
                    fulfillVerifiedPurchase(purchase)
                }.onFailure {
                    binding.purchaseStatus.visibility = View.VISIBLE
                    binding.purchaseStatus.text = "Purchase verification failed. Please try again."
                    Toast.makeText(this, "Purchase verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun fulfillVerifiedPurchase(purchase: Purchase) {
        val purchasedProducts = purchase?.products.orEmpty()
        if (purchasedProducts.any { it.isSubscriptionProductId() }) {
            Toast.makeText(this, "Subscription active", Toast.LENGTH_SHORT).show()
        }

        val coins = purchasedProducts.sumOf { productId ->
            when (productId) {
                COINS_50_PRODUCT_ID -> 50
                COINS_100_PRODUCT_ID -> 100
                COINS_200_PRODUCT_ID -> 200
                COINS_500_PRODUCT_ID -> 500
                STARTER_BUNDLE_PRODUCT_ID -> 150
                else -> 0
            }
        }

        if (coins > 0) {
            coinManager.addCoins(coins)
            val itemCount = purchasedProducts.count { !it.isSubscriptionProductId() }
            val itemLabel = if (itemCount > 1) "$itemCount items" else "1 item"
            Toast.makeText(this, "Purchase Successful: +$coins coins from $itemLabel", Toast.LENGTH_SHORT).show()
        }

        // Mark the purchased item as consumed to allow repurchase
        purchaseManager.consumePurchase(purchase)
    }

    private fun String.isSubscriptionProductId(): Boolean {
        return this == PREMIUM_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID ||
            this == PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
    }

    private fun List<ProductDetails?>?.hasPremiumSubscriptionAddOns(): Boolean {
        val productIds = this
            ?.filterNotNull()
            ?.filter { !it.subscriptionOfferDetails.isNullOrEmpty() }
            ?.map { it.productId }
            ?.toSet()
            .orEmpty()
        return productIds.contains(PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID) &&
            productIds.contains(PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID) &&
            productIds.contains(PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID)
    }

    override fun onPurchaseFailed(billingResult: BillingResult?, errorMessage: String?) {
        val message = errorMessage ?: "Purchase failed. Please try again."
        binding.purchaseStatus.visibility = View.VISIBLE
        binding.purchaseStatus.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPurchasePending(purchase: Purchase?) {
        Toast.makeText(this, "Purchase Pending...", Toast.LENGTH_SHORT).show()
    }

    override fun onPurchaseCanceled() {
        Toast.makeText(this, "Purchase Canceled", Toast.LENGTH_SHORT).show()
    }

    private fun configureScreenForProductGroup() {
        when (productGroup) {
            PRODUCT_GROUP_PREMIUM -> {
                binding.toolbarTitle.text = "Premium Features"
                binding.screenTitle.text = "Premium Subscriptions"
                binding.screenSubtitle.text = "Choose one premium tier."
                binding.purchaseStatus.text = "Loading premium subscriptions..."
            }
            PRODUCT_GROUP_BUNDLE -> {
                binding.toolbarTitle.text = "Bundles"
                binding.screenTitle.text = "Bundles"
                binding.screenSubtitle.text = "Use one checkout for bundled coins, or one checkout for all premium add-ons."
                binding.purchaseStatus.text = "Loading bundle offers..."
            }
            else -> {
                binding.toolbarTitle.text = "Purchase Coins"
                binding.screenTitle.text = "One-Time Coin Packs"
                binding.screenSubtitle.text = "Each pack unlocks coins immediately. Buy again anytime."
                binding.purchaseStatus.text = "Loading coin products..."
            }
        }
    }

    companion object {
        const val EXTRA_PRODUCT_GROUP = "extra_product_group"
        const val PRODUCT_GROUP_COINS = "coins"
        const val PRODUCT_GROUP_PREMIUM = "premium"
        const val PRODUCT_GROUP_BUNDLE = "bundle"
    }
}
