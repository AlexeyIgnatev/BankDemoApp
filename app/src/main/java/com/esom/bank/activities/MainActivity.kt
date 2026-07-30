package com.esom.bank.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.NavOptions
import com.esom.bank.R
import com.esom.bank.databinding.ActivityMainBinding
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private var lastInteractionAt = SystemClock.elapsedRealtime()
    private var isActivityResumed = false
    private var lockRequested = false

    @Inject
    lateinit var pinLocalDataSource: PinLocalDataSource

    private val inactivityRunnable = Runnable {
        if (isActivityResumed) {
            lockIfRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars =
            true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        lockIfRequired()
        scheduleInactivityCheck()
    }

    override fun onPause() {
        isActivityResumed = false
        inactivityHandler.removeCallbacks(inactivityRunnable)
        super.onPause()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            lastInteractionAt = SystemClock.elapsedRealtime()
            lockRequested = false
            scheduleInactivityCheck()
        }
        return super.dispatchTouchEvent(event)
    }

    fun markUserAuthenticated() {
        lastInteractionAt = SystemClock.elapsedRealtime()
        lockRequested = false
        scheduleInactivityCheck()
    }

    private fun scheduleInactivityCheck() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        if (!isActivityResumed || isLockDestination()) return

        val elapsed = SystemClock.elapsedRealtime() - lastInteractionAt
        inactivityHandler.postDelayed(
            inactivityRunnable,
            (INACTIVITY_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
        )
    }

    private fun lockIfRequired() {
        if (lockRequested || isLockDestination()) return
        if (SystemClock.elapsedRealtime() - lastInteractionAt < INACTIVITY_TIMEOUT_MS) return
        if (!pinLocalDataSource.hasLock()) return

        lockRequested = true
        findNavController(R.id.nav_host_fragment).navigate(
            R.id.logInFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }

    private fun isLockDestination(): Boolean {
        return findNavController(R.id.nav_host_fragment).currentDestination?.id in setOf(
            R.id.authFragment,
            R.id.logInFragment,
            R.id.pinCreateFragment,
            R.id.registrationFragment,
            R.id.smsFragment,
            R.id.bioFragment,
            R.id.splashLogInFragment
        )
    }

    private companion object {
        const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
