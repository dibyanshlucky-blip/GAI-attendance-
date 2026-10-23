package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdMob Configuration & Test IDs
 * Note: Always test with official Google test IDs during development.
 * Replace with your real AdMob Unit IDs before publishing on Google Play Store.
 */
object AdMobConstants {
    // User's Real AdMob Configuration
    const val REAL_APP_ID = "ca-app-pub-8343909811697785~6013716256"
    const val REAL_BANNER_AD_ID = "ca-app-pub-8343909811697785/9761389575"

    // Official Google Test Ad Unit IDs
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    // Active Ad Unit IDs (Real Banner ID connected)
    var BANNER_AD_UNIT_ID = REAL_BANNER_AD_ID
    var INTERSTITIAL_AD_UNIT_ID = TEST_INTERSTITIAL_AD_ID
}

/**
 * AdMob Manager to handle SDK initialization and Interstitial Ads
 */
object AdMobManager {
    private const val TAG = "AdMobManager"
    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    /**
     * Initializes the Mobile Ads SDK once per app lifecycle
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Configure emulator as test device so real IDs can be tested safely without policy violation
            val testConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(testConfig)

            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob SDK Initialized: ${initializationStatus.adapterStatusMap}")
                isInitialized = true
                preloadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed", e)
        }
    }

    /**
     * Preloads an Interstitial Ad in background
     */
    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdMobConstants.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w(TAG, "Failed to load interstitial ad: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows the preloaded Interstitial Ad if available, then executes the onFinished callback
     */
    fun showInterstitial(activity: Activity, onFinished: () -> Unit) {
        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onFinished()
                }
            }
            currentAd.show(activity)
        } else {
            // No ad ready, proceed immediately without blocking user
            preloadInterstitial(activity)
            onFinished()
        }
    }
}

/**
 * Reusable Jetpack Compose Banner Ad Component.
 * Integrates Google AdMob banner with automated lifecycle handling and smooth error recovery.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConstants.BANNER_AD_UNIT_ID
) {
    val isInspection = LocalInspectionMode.current
    var isAdLoaded by remember { mutableStateOf(false) }

    if (isInspection) {
        // Compose Preview mode placeholder
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AdMob Banner Preview", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_banner_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional subtle sponsored label
        Row(
            modifier = Modifier
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "AD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isAdLoaded = true
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                isAdLoaded = false
                                Log.d("AdMobBanner", "Ad failed to load (${error.code}): ${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { adView ->
                    adView.destroy()
                }
            )
        }
    }
}
