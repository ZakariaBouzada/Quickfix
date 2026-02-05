package com.example.bikerepairapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

class BIkeRepairApp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val root = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // This view exists (it's the FragmentContainerView/fragment host in your layout)
        val navHostView = findViewById<android.view.View>(R.id.nav_host_fragment)

        // ✅ Apply ONLY what you need:
        // - top inset -> content (so it doesn't go under status bar)
        // - bottom inset -> bottom nav (so icons aren't clipped by gesture bar)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // keep content below status bar
            navHostView.setPadding(0, sys.top, 0, 0)

            // keep bottom nav above gesture bar + avoid clipping
            bottomNav.setPadding(
                bottomNav.paddingLeft,
                bottomNav.paddingTop,
                bottomNav.paddingRight,
                sys.bottom
            )

            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)

        val openBooking = intent.getBooleanExtra("OPEN_BOOKING", false)
        if (openBooking) {
            bottomNav.selectedItemId = R.id.bookingFragment
        }
    }
}
