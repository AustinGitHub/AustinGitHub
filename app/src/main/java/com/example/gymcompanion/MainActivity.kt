package com.example.gymcompanion

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductType
import kotlinx.coroutines.launch
import java.time.Duration

class MainActivity : ComponentActivity(), PurchasesUpdatedListener {

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled by UI */ }

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()
        setContent { GymCompanionApp(onEnableReminders = { scheduleReminders() }, onPurchase = { startBillingFlow() }) }
        setupBilling()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleReminders() {
        val workManager = WorkManager.getInstance(this)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofHours(24))
            .build()
        workManager.enqueueUniquePeriodicWork(
            "gym-reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscription()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry automatically when the user re-opens the app.
            }
        })
    }

    private fun querySubscription() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("gym_companion_pro")
                .setProductType(ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            // In a production app you would store these details and show them dynamically.
        }
    }

    private fun startBillingFlow() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("gym_companion_pro")
                .setProductType(ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.firstOrNull()?.let { productDetails ->
                    launchBillingFlow(productDetails)
                }
            }
        }
    }

    private fun launchBillingFlow(productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingClient.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingClient.BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        // In a production app you would acknowledge purchases and unlock premium content.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymCompanionApp(onEnableReminders: () -> Unit, onPurchase: () -> Unit) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Gym Companion") }, navigationIcon = {
                    Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null)
                })
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Content(onEnableReminders = onEnableReminders, onPurchase = onPurchase)
            }
        }
    }
}

@Composable
private fun Content(onEnableReminders: () -> Unit, onPurchase: () -> Unit) {
    val reminderStatus = remember { mutableStateOf("Daily reminders are off") }
    val subscriptionStatus = remember { mutableStateOf("Pro not purchased") }

    LaunchedEffect(Unit) {
        reminderStatus.value = "Daily reminders are ready"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Text(text = "Stay on track", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Enable daily reminders so you never miss a workout.")
                    Button(onClick = {
                        onEnableReminders()
                        reminderStatus.value = "Daily reminders scheduled"
                    }) {
                        Text("Enable reminders")
                    }
                    Text(reminderStatus.value, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                    Text(text = "Unlock pro guidance", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Subscribe for weekly programs, tips, and unlimited notification slots.")
                    Button(onClick = {
                        onPurchase()
                        subscriptionStatus.value = "Purchase flow started"
                    }) {
                        Text("Subscribe")
                    }
                    Text(subscriptionStatus.value, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("This starter experience includes:", fontWeight = FontWeight.Bold)
                    Bullet("A simple dashboard to keep motivation high")
                    Bullet("Daily notification reminders powered by WorkManager")
                    Bullet("A subscription entry point using Google Play Billing")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Fill in your own workout lessons, schedules, and pro content to publish quickly.")
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text(text = "• $text")
}
