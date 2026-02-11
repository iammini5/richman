package com.legendsoftware.richmangoogleplaybillinglibrarytest.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.PurchaseCoinBinding

class PurchaseProductAdapter(
    private var productList: List<ProductDetails>,
    private val onPurchaseClick: (ProductDetails) -> Unit
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
    fun setProducts(products: List<ProductDetails>) {
        productList = products
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: PurchaseCoinBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductDetails) {
            binding.title.text = product.name
            binding.description.text = product.description
            binding.price.text = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "Loading..."
            binding.btnBuy.setOnClickListener { onPurchaseClick(product) }
        }
    }
}
