package com.colbycoapps.med_standards.ui.about

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.colbycoapps.med_standards.databinding.FragmentSubscriptionBinding
import com.colbycoapps.med_standards.ui.Utils

class SubscriptionFragment : Fragment() {

    private lateinit var binding: FragmentSubscriptionBinding
    private lateinit var billingClient: BillingClient

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {

        } else {

        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // SUBSCRIPTION DISABLED - Billing client setup disabled
        // setupBillingClient(requireContext())
        // binding.buttonSelect.setOnClickListener {
        //     launchSubscriptionPurchaseFlow(requireActivity(), "premium_subscription")
        // }
        
        // Disable subscription button
        // binding.buttonSelect.isEnabled = false
        // binding.buttonSelect.text = "Free App"

        return root
    }

    private fun setupBillingClient(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

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
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Utils.countFree = 10
                        Utils.premium = true
                    }
                }
            }
        }
    }


}