package com.esom.bank.activities

import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Color
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.esom.bank.R
import com.esom.bank.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(viewModel.getThemeMode())
        enableEdgeToEdge()

        val isNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNight
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNight

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStartDestination()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onActivityResumed(isLockDestination())
    }

    override fun onPause() {
        viewModel.onActivityPaused()
        super.onPause()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            viewModel.onUserInteraction(isLockDestination())
        }
        return super.dispatchTouchEvent(event)
    }

    fun markUserAuthenticated() {
        viewModel.markUserAuthenticated()
    }

    private fun setupStartDestination() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val controller = navHost.navController
        val graph = controller.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            when {
                !viewModel.isAuthenticated() -> R.id.authFragment
                viewModel.hasLock() -> R.id.logInFragment
                else -> R.id.pinCreateFragment
            }
        )
        controller.graph = graph
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.shouldLock || isLockDestination()) return@collect
                    findNavController(R.id.nav_host_fragment).navigate(
                        R.id.logInFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                    viewModel.onLockHandled()
                }
            }
        }
    }

    private fun isLockDestination(): Boolean {
        return findNavController(R.id.nav_host_fragment).currentDestination?.id in setOf(
            R.id.authFragment,
            R.id.logInFragment,
            R.id.pinCreateFragment,
            R.id.bioFragment
        )
    }
}
