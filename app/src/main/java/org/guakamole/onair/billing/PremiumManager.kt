package org.guakamole.onair.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.*
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manages premium feature unlocking via Google Play Billing.
 *
 * This is a singleton that handles:
 * - BillingClient connection lifecycle
 * - Purchase state queries and restoration
 * - Purchase flow initiation
 * - Local caching for offline access
 */
class PremiumManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "PremiumManager"
        const val PRODUCT_ID = "premium_unlock"
        private const val PREFS_NAME = "premium_prefs"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_DEBUG_PREMIUM = "debug_premium"

        @Volatile private var instance: PremiumManager? = null

        fun getInstance(context: Context): PremiumManager {
            return instance
                    ?: synchronized(this) {
                        instance
                                ?: PremiumManager(context.applicationContext).also { instance = it }
                    }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var billingClient: BillingClient? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null
        ) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
        _isLoading.value = false
    }

    init {
        // Check for debug override
        if (BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_PREMIUM, false)) {
            _isPremium.value = true
        }

        scope.launch { initialize() }
    }

    /** Initialize the billing client and restore any existing purchases. */
    suspend fun initialize() {
        if (billingClient?.isReady == true) return

        billingClient =
                BillingClient.newBuilder(context)
                        .setListener(purchasesUpdatedListener)
                        .enablePendingPurchases(
                                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                        )
                        .build()

        val connected = connectBillingClient()
        if (connected) {
            queryProductDetails()
            restorePurchases()
        }
    }

    private suspend fun connectBillingClient(): Boolean = suspendCancellableCoroutine { cont ->
        billingClient?.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Billing client connected")
                            cont.resume(true)
                        } else {
                            Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                            cont.resume(false)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Log.w(TAG, "Billing service disconnected")
                        // Reconnect on next operation
                    }
                }
        )
    }

    private suspend fun queryProductDetails() {
        val client = billingClient ?: return

        val productList =
                listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
            Log.d(TAG, "Product details loaded: ${_productDetails.value?.name}")
        } else {
            Log.e(TAG, "Failed to query product details: ${result.billingResult.debugMessage}")
        }
    }

    /** Restore purchases from Google Play. */
    suspend fun restorePurchases() {
        val client = billingClient ?: return

        val params =
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()

        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasPremium =
                    result.purchasesList.any { purchase ->
                        purchase.products.contains(PRODUCT_ID) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }

            if (hasPremium) {
                setPremiumState(true)
                Log.d(TAG, "Premium purchase restored")
            } else {
                // Only clear if we didn't have a debug override
                if (!BuildConfig.DEBUG || !prefs.getBoolean(KEY_DEBUG_PREMIUM, false)) {
                    setPremiumState(false)
                }
            }

            // Acknowledge any unacknowledged purchases
            for (purchase in result.purchasesList) {
                if (!purchase.isAcknowledged &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    /** Launch the purchase flow for the premium unlock. */
    fun purchase(activity: Activity) {
        val client =
                billingClient
                        ?: run {
                            Log.e(TAG, "Billing client not initialized")
                            return
                        }

        val details =
                _productDetails.value
                        ?: run {
                            Log.e(TAG, "Product details not available")
                            // Try to reconnect and reload
                            scope.launch { initialize() }
                            return
                        }

        _isLoading.value = true

        val productDetailsParamsList =
                listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                )

        val billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

        val result = client.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.debugMessage}")
            _isLoading.value = false
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_ID)) {
                setPremiumState(true)
                Log.d(TAG, "Premium unlocked!")

                if (!purchase.isAcknowledged) {
                    scope.launch { acknowledgePurchase(purchase) }
                }
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return

        val params =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

        val result = client.acknowledgePurchase(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchase acknowledged")
        } else {
            Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
        }
    }

    private fun setPremiumState(isPremium: Boolean) {
        _isPremium.value = isPremium
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    }

    /** Get the formatted price for the premium unlock. */
    fun getFormattedPrice(): String? {
        return _productDetails.value?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Debug only: Toggle premium state for testing without real purchases. This has no effect in
     * release builds.
     */
    fun debugSetPremium(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return

        prefs.edit().putBoolean(KEY_DEBUG_PREMIUM, enabled).apply()
        _isPremium.value = enabled
        Log.d(TAG, "Debug premium set to: $enabled")
    }

    /** Debug only: Check if debug premium is enabled. */
    fun isDebugPremiumEnabled(): Boolean {
        return BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_PREMIUM, false)
    }
}

/**
 * Placeholder for BuildConfig - this will be generated by the build system. We define it here to
 * avoid compilation errors before the first build.
 */
private object BuildConfig {
    const val DEBUG = true
}
