package com.legendsoftware.richmangoogleplaybillinglibrarytest.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.legendsoftware.richmangoogleplaybillinglibrarytest.R
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseOption
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.COINS_100_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.COINS_200_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.COINS_500_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.COINS_50_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_BASIC_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_PLUS_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_PRO_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseProducts.STARTER_BUNDLE_PRODUCT_ID
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.PurchaseCoinBinding

class PurchaseProductAdapter(
    private var productList: List<PurchaseOption>,
    private val onPurchaseClick: (PurchaseOption) -> Unit
) : RecyclerView.Adapter<PurchaseProductAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PurchaseCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    @SuppressLint("NotifyDataSetChanged")
    fun setProducts(products: List<PurchaseOption>) {
        productList = products
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: PurchaseCoinBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(option: PurchaseOption) {
            val product = option.productDetails
            val isSubscription = option.isSubscription
            binding.coinIcon.setImageResource(iconFor(product.productId, isSubscription))
            binding.title.text = titleFor(option, isSubscription)
            binding.description.text = entitlementFor(option, isSubscription)
            val subscriptionPrice = option.subscriptionOffer
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
            binding.price.text = if (product.productId == STARTER_BUNDLE_PRODUCT_ID) {
                "Multi-item checkout"
            } else product.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                ?: subscriptionPrice
                ?: "Loading..."
            binding.btnBuy.text = when {
                product.productId == STARTER_BUNDLE_PRODUCT_ID -> "Buy Bundle"
                !isSubscription -> "Buy Now"
                option.isMonthlyBasePlan -> "Switch to Monthly"
                option.isYearlyBasePlan -> "Choose Yearly"
                else -> "Subscribe"
            }
            binding.btnBuy.setOnClickListener { onPurchaseClick(option) }
        }

        private fun titleFor(option: PurchaseOption, isSubscription: Boolean): String {
            val planLabel = when {
                option.isMonthlyBasePlan -> "Monthly"
                option.isYearlyBasePlan -> "Yearly"
                else -> "Subscription"
            }

            return when (option.productId) {
                PREMIUM_BASIC_SUBSCRIPTION_ID -> "Basic Premium $planLabel"
                PREMIUM_PLUS_SUBSCRIPTION_ID -> "Plus $planLabel"
                PREMIUM_PRO_SUBSCRIPTION_ID -> "Pro $planLabel"
                PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID -> "Basic Monthly"
                PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID -> "Plus Monthly"
                PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID -> "Pro Monthly"
                PREMIUM_MONTHLY_SUBSCRIPTION_ID -> "Premium Monthly"
                STARTER_BUNDLE_PRODUCT_ID -> "Starter Bundle"
                else -> if (isSubscription) "Premium $planLabel" else option.productDetails.name
            }
        }

        private fun entitlementFor(option: PurchaseOption, isSubscription: Boolean): String {
            val product = option.productDetails
            val cadence = when {
                option.isMonthlyBasePlan -> "Monthly billing. If switching from yearly, the monthly plan is charged now."
                option.isYearlyBasePlan -> "Yearly billing with better value."
                else -> null
            }
            val entitlement = when (product.productId) {
                COINS_50_PRODUCT_ID -> "Entitlement: 50 coins for quick boosts and small actions."
                COINS_100_PRODUCT_ID -> "Entitlement: 100 coins for extra turns and steady progress."
                COINS_200_PRODUCT_ID -> "Entitlement: 200 coins for longer sessions and bigger moves."
                COINS_500_PRODUCT_ID -> "Entitlement: 500 coins for power play and maximum flexibility."
                STARTER_BUNDLE_PRODUCT_ID -> "Multi-item checkout: Starter Bundle + 100 Coins + 200 Coins in one Play purchase."
                PREMIUM_BASIC_SUBSCRIPTION_ID,
                PREMIUM_BASIC_MONTHLY_SUBSCRIPTION_ID -> "Entitlement: Basic premium tools."
                PREMIUM_PLUS_SUBSCRIPTION_ID,
                PREMIUM_PLUS_MONTHLY_SUBSCRIPTION_ID -> "Entitlement: Plus boosters and richer play."
                PREMIUM_PRO_SUBSCRIPTION_ID,
                PREMIUM_PRO_MONTHLY_SUBSCRIPTION_ID -> "Entitlement: Pro access and top-tier perks."
                PREMIUM_MONTHLY_SUBSCRIPTION_ID -> product.description.ifBlank { "Entitlement: Premium Rich Man features every month." }
                else -> if (isSubscription) {
                    product.description.ifBlank { "Entitlement: Premium Rich Man features every month." }
                } else {
                    product.description
                }
            }
            return listOfNotNull(entitlement, cadence).joinToString(" ")
        }

        private fun iconFor(productId: String, isSubscription: Boolean): Int {
            return when (productId) {
                COINS_50_PRODUCT_ID -> android.R.drawable.ic_menu_add
                COINS_100_PRODUCT_ID -> android.R.drawable.ic_menu_upload
                COINS_200_PRODUCT_ID -> android.R.drawable.ic_menu_compass
                COINS_500_PRODUCT_ID -> android.R.drawable.star_big_on
                STARTER_BUNDLE_PRODUCT_ID -> android.R.drawable.ic_menu_myplaces
                else -> if (isSubscription) android.R.drawable.ic_menu_manage else R.drawable.ic_coin
            }
        }
    }
}
