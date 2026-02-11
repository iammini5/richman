package com.legendsoftware.richmangoogleplaybillinglibrarytest.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.legendsoftware.richmangoogleplaybillinglibrarytest.prefs.SecureCoinManager

class CoinManagerViewModel : ViewModel() {
    private val _coins = MutableLiveData<Int>()
    val coins: LiveData<Int> = _coins

    init {
        _coins.value = SecureCoinManager.getCoins()
    }

    fun addCoins(amount: Int) {
        SecureCoinManager.addCoins(amount)
        _coins.value = SecureCoinManager.getCoins()
    }
}