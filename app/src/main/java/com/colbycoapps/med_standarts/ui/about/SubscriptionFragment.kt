package com.colbycoapps.med_standarts.ui.about

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.colbycoapps.med_standarts.R
import com.colbycoapps.med_standarts.databinding.FragmentAboutAppBinding
import com.colbycoapps.med_standarts.databinding.FragmentAirforseBinding
import com.colbycoapps.med_standarts.databinding.FragmentDodBinding
import com.colbycoapps.med_standarts.databinding.FragmentNavyBinding
import com.colbycoapps.med_standarts.databinding.FragmentSubscriptionBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.colbycoapps.med_standarts.ui.adapter.FileAdapter
import com.colbycoapps.med_standarts.ui.dod.DodViewModel
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity

class SubscriptionFragment : Fragment() {

    private lateinit var binding: FragmentSubscriptionBinding
    private lateinit var billingClient: BillingClient

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // користувач скасував
        } else {
            // інша помилка
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupBillingClient(requireContext())
        binding.buttonSelect.setOnClickListener {
            launchSubscriptionPurchaseFlow(requireActivity(), "premium_subscription")
        }

        return root
    }

    private fun setupBillingClient(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases() // обов’язково для Billing Library 2.0+
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // Спробуйте повторно підключитися
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Готові запитувати покупки, SKU details, тощо.
                }
            }
        })
    }

    fun launchSubscriptionPurchaseFlow(activity: Activity, skuId: String) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(skuId))
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList[0])
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Переконайтеся, що покупку підтверджено
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Підписка підтверджена
                    }
                }
            }
        }
    }


}