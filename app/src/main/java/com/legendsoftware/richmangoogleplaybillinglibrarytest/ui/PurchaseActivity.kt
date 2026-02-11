package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import android.os.Bundle
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
import com.legendsoftware.richmangoogleplaybillinglibrarytest.adapter.PurchaseProductAdapter
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseUpdateListener
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.ActivityPurchaseBinding
import com.legendsoftware.richmangoogleplaybillinglibrarytest.viewmodel.CoinManagerViewModel

class PurchaseActivity : AppCompatActivity(), PurchaseUpdateListener {
    private lateinit var binding: ActivityPurchaseBinding
    private val coinManager: CoinManagerViewModel by viewModels()
    private lateinit var purchaseManager: RichmanPurchaseManager
    private lateinit var adapter: PurchaseProductAdapter

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
        purchaseManager = RichmanPurchaseManager(this, this)

        adapter = PurchaseProductAdapter(emptyList()) { productDetails ->
            purchaseManager.launchPurchase(productDetails.productId)
        }

        binding.inAppProducts.layoutManager = LinearLayoutManager(this)
        binding.inAppProducts.adapter = adapter

        coinManager.coins.observe(this) { coins ->
            binding.coins.text = coins.toString()
        }

    }

    override fun onProductsLoaded(productDetailsList: List<ProductDetails?>?) {
        val safeList = productDetailsList?.filterNotNull() ?: emptyList()
        adapter.setProducts(safeList)
    }

//    override fun onProductsLoaded(productDetailsList: List<ProductDetails?>?) {
//        val safeList = productDetailsList?.filterNotNull() ?: emptyList()
//        val sortedList = purchaseManager.sortProductsByPriceSequence(safeList)
//        adapter.setProducts(sortedList)
//    }


    override fun onPurchaseSuccess(purchase: Purchase?) {
        // Update user account with purchased coins
        purchase?.products?.getOrNull(0)?.let { productId ->
            val coins = when (productId) {
                "com.legendsoftware.richman.coins.50" -> 50
                "com.legendsoftware.richman.coins.100" -> 100
                "com.legendsoftware.richman.coins.200" -> 200
                "com.legendsoftware.richman.coins.500" -> 500
                else -> 0
            }
            coinManager.addCoins(coins)
            Toast.makeText(this, "Purchase Successful: +$coins coins", Toast.LENGTH_SHORT).show()
        }

        // Mark the purchased item as consumed to allow repurchase
        purchaseManager.consumePurchase(purchase)
    }

    override fun onPurchaseFailed(billingResult: BillingResult?, errorMessage: String?) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onPurchasePending(purchase: Purchase?) {
        Toast.makeText(this, "Purchase Pending...", Toast.LENGTH_SHORT).show()
    }

    override fun onPurchaseCanceled() {
        Toast.makeText(this, "Purchase Canceled", Toast.LENGTH_SHORT).show()
    }
}
