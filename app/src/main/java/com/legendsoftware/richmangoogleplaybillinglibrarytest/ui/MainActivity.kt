package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.PurchaseUpdateListener
import com.legendsoftware.richmangoogleplaybillinglibrarytest.billing.RichmanPurchaseManager
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.ActivityMainBinding
import com.legendsoftware.richmangoogleplaybillinglibrarytest.viewmodel.CoinManagerViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val coinManager: CoinManagerViewModel by viewModels()
    private lateinit var purchaseManager: RichmanPurchaseManager

    // in-app update
    private lateinit var appUpdateManager: AppUpdateManager
    private val appUpdateType: Int = AppUpdateType.IMMEDIATE
    private lateinit var activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var isUpdateStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //=============== main ===============//
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //========== show current coins dynamically ==========
        coinManager.coins.observe(this) { coins ->
            binding.coins.text = coins.toString()
        }

        // in-app purchase
        // purchaseManager = RichmanPurchaseManager(this, this)

        //========== buy coins ==========//
        binding.buyCoins.setOnClickListener {
            startActivity(
                Intent(this, PurchaseActivity::class.java)
                    .putExtra(PurchaseActivity.EXTRA_PRODUCT_GROUP, PurchaseActivity.PRODUCT_GROUP_COINS)
            )
        }

        //========== remove ads ==========//
        binding.removeAds.setOnClickListener { }

        //========== premium features ==========//
        binding.premiumFeatures.setOnClickListener {
            startActivity(
                Intent(this, PurchaseActivity::class.java)
                    .putExtra(PurchaseActivity.EXTRA_PRODUCT_GROUP, PurchaseActivity.PRODUCT_GROUP_PREMIUM)
            )
        }

        //========== more ==========//
        binding.more.setOnClickListener {
            startActivity(
                Intent(this, PurchaseActivity::class.java)
                    .putExtra(PurchaseActivity.EXTRA_PRODUCT_GROUP, PurchaseActivity.PRODUCT_GROUP_BUNDLE)
            )
        }


        // in-app update
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                if (result.resultCode != RESULT_OK) {
                    // val msg = "Update flow failed! Result code: " + result.resultCode
                    // If the update is canceled or fails,
                    // you can request to start the update again.
                }
            }

        checkForUpdate()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(appUpdateType).build()
                )
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!isUpdateStarted &&
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(appUpdateType)
            ) {
                isUpdateStarted = true
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(appUpdateType).build()
                )
            }
        }
    }

//    override fun onPurchaseUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
//
//        when (billingResult.responseCode) {
//            BillingClient.BillingResponseCode.OK if purchases != null -> {
//                for (purchase in purchases) {
//                    if (purchase.products.contains("com.legendsoftware.richman.coins.500")) {
//                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
//                            coinManager.addCoins(500)
//                            purchaseManager.consumePurchase(purchase)
//                        }
//                    }
//                }
//            }
//
//            BillingClient.BillingResponseCode.USER_CANCELED -> {
//                Log.d("billing", "Purchase canceled by user")
//            }
//
//            else -> {
//                Log.d("billing", "Purchase failed: ${billingResult.debugMessage}")
//            }
//        }
//    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
