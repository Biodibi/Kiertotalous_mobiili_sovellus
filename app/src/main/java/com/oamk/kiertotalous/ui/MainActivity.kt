package com.oamk.kiertotalous.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.allViews
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.oamk.kiertotalous.App
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.ActivityMainBinding
import com.oamk.kiertotalous.model.UserRole
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    private val viewModel: MainViewModel by viewModel()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private var _viewDataBinding: ActivityMainBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        installSplashScreen()

        _viewDataBinding = ActivityMainBinding.inflate(layoutInflater)
        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = this
        viewDataBinding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.green))

        setContentView(viewDataBinding.root)
        setSupportActionBar(viewDataBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(this)
        updateNavigationGraph()

        registerObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: Remove after it's verified all components are cleared properly
        exitProcess(0)
    }

    private fun registerObservers() {
        viewModel.navInfoLiveData.observe(this) { _navInfo ->
            _navInfo?.let { navInfo ->
                if (navInfo.destinationId != navController.currentDestination?.id) {
                    if (navInfo.navDirections != LoginFragmentDirections.actionGlobalLoginFragment() && navController.currentDestination?.id == R.id.loginFragment) {
                        updateNavigationGraph()
                    } else {
                        navController.navigate(navInfo.navDirections)
                    }
                }
            }
        }

        viewModel.appNotificationLiveData.observe(this) { _notification ->
            _notification?.let { notification ->
                val snackbarView = Snackbar.make(findViewById(android.R.id.content), notification.message , Snackbar.LENGTH_LONG)
                snackbarView.duration = notification.duration
                snackbarView.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
                val snackbarLayout = snackbarView.view as Snackbar.SnackbarLayout
                snackbarLayout.allViews.forEach { view ->
                    if (view is AppCompatTextView) {
                        if (_notification.isError) {
                            view.setTextColor(getColor(R.color.error_red))
                        }
                        view.textAlignment = (View.TEXT_ALIGNMENT_CENTER)
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22F)
                    }
                }

                val params = snackbarLayout.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP and Gravity.CENTER_HORIZONTAL
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                snackbarLayout.layoutParams = params
                snackbarLayout.elevation = 5F
                snackbarLayout.setPadding(0, 7, 0, 7)
                snackbarView.show()
            }
        }
    }

    private fun updateNavigationGraph() {
        var startDestinationId = R.id.loginFragment
        viewModel.userAccount?.userRole()?.let { role ->
            startDestinationId = when (role) {
                UserRole.ADMIN, // No admin screen needed at the moment
                UserRole.COURIER -> R.id.deliveriesFragment
                UserRole.STORE -> R.id.deliveryFormFragment
            }
        }

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(startDestinationId)
        navController.setGraph(graph, null)
        appBarConfiguration = AppBarConfiguration.Builder(graph).build()
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        viewDataBinding.toolbar.isVisible = destination.id != R.id.loginFragment

        when (destination.id) {
            R.id.deliveriesFragment,
            R.id.summaryFragment -> title = getString(R.string.title_deliveries)
            R.id.deliveryFormFragment -> title = getString(R.string.title_form)
            R.id.settingsFragment -> title = getString(R.string.title_settings)
        }
    }
}