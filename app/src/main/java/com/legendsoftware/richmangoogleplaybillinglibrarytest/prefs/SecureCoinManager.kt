package com.legendsoftware.richmangoogleplaybillinglibrarytest.prefs

import androidx.core.content.edit

import android.content.Context
import android.content.SharedPreferences

object SecureCoinManager {

    private const val PREF_NAME = "coins_prefs"
    private const val KEY_COINS = "coins"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCoins(): Int {
        return sharedPreferences.getInt(KEY_COINS, 0)
    }

    fun setCoins(coins: Int) {
        sharedPreferences.edit { putInt(KEY_COINS, coins) }
    }

    fun addCoins(amount: Int) {
        setCoins(getCoins() + amount)
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        return if (current >= amount) {
            setCoins(current - amount)
            true
        } else false
    }
}
