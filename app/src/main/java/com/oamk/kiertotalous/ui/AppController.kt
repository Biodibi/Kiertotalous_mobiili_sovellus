package com.oamk.kiertotalous.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.StrictMode
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.oamk.kiertotalous.BuildConfig
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.data.DeviceRepository
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.model.AppNotification
import com.oamk.kiertotalous.model.NavInfo
import com.oamk.kiertotalous.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class AppController(private val _context: Context,
                    private val localDataRepository: LocalDataRepository,
                    private val remoteDataRepository: RemoteDataRepository,
                    private val deviceRepository: DeviceRepository,
                    private val firebaseAuth: FirebaseAuth,
                    private val firebaseCrashlytics: FirebaseCrashlytics
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.Main

    val context = _context
    private val navInfoMutableStateFlow = MutableStateFlow<NavInfo?>(null)
    val navInfoStateFlow = navInfoMutableStateFlow.asStateFlow()

    private val appNotificationMutableStateFlow = MutableStateFlow<AppNotification?>(null)
    val appNotificationStateFlow = appNotificationMutableStateFlow.asStateFlow()

    init {
        /* if (BuildConfig.DEBUG) {
            // Useful for tracking memory leaks etc
            logPolicyViolations()
        } */
        // Initialize Firebase
        FirebaseApp.initializeApp(context)

        firebaseAuth.addAuthStateListener { auth ->
            if (auth.currentUser == null && localDataRepository.userAccount != null) {
                logout()
            }
        }

        launch {
            localDataRepository.userAccountStateFlow.collect { newUserAccount ->
                localDataRepository.userAccount?.userId?.let { userUuid ->
                    firebaseCrashlytics.setUserId(userUuid)
                }
                newUserAccount?.userRole()?.let { role ->
                    when (role) {
                        UserRole.ADMIN, // No admin screen needed at the moment
                        UserRole.COURIER -> navigate(NavInfo(R.id.deliveriesFragment, DeliveriesFragmentDirections.actionGlobalDeliveriesFragment()))
                        UserRole.STORE -> navigate(NavInfo(R.id.deliveryFormFragment, DeliveriesFragmentDirections.actionGlobalDeliveryFormFragment()))
                    }
                }
            }
        }

        plantTimber()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationManager = context.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(Const.CHANNEL_ID, Const.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.description = Const.CHANNEL_DESCRIPTION
        notificationChannel.setShowBadge(true)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun plantTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return super.createStackElementTag(element) + ":" + element.lineNumber
                }
            })
        }
    }

    private fun logPolicyViolations() {
        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setVmPolicy(vmPolicy)

        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setThreadPolicy(threadPolicy)
    }

    fun logout() {
        launch {
            Timber.d("Logging out")
            showAppNotification(AppNotification(message = R.string.logout_inprogress, duration = 10000))
            (context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
            deviceRepository.logout()
            localDataRepository.logout()
            remoteDataRepository.logout()

            delay(1000)
            // TODO: Replace exitProcess() with restart() if after it's verified all components are cleared properly
            exitProcess(0) // (context as App).restart()
        }
    }

    fun navigate(navInfo: NavInfo) {
        launch {
            if (navInfoMutableStateFlow.value != navInfo) {
                navInfoMutableStateFlow.value = navInfo
            }
            navInfoMutableStateFlow.value = null
        }
    }

    fun showAppNotification(notification: AppNotification) {
        launch {
            if (appNotificationMutableStateFlow.value != notification) {
                appNotificationMutableStateFlow.value = notification
            }
            appNotificationMutableStateFlow.value = null
        }
    }
}