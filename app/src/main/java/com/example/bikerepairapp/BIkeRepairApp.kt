package com.example.bikerepairapp

import android.os.Bundle
import android.view.View
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
        val navHostView = findViewById<View>(R.id.nav_host_fragment)
        val bottomPlate = findViewById<View>(R.id.bottomPlate)

        val navOrigTop = navHostView.paddingTop
        val plateOrigPaddingBottom = bottomPlate.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // content below status bar
            navHostView.setPadding(
                navHostView.paddingLeft,
                navOrigTop + sys.top,
                navHostView.paddingRight,
                navHostView.paddingBottom
            )

            // fill gesture/navigation-bar area with the plate
            bottomPlate.setPadding(
                bottomPlate.paddingLeft,
                bottomPlate.paddingTop,
                bottomPlate.paddingRight,
                plateOrigPaddingBottom + sys.bottom
            )

            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)

        val openBooking = intent.getBooleanExtra("OPEN_BOOKING", false)
        if (openBooking) {
            bottomNav.selectedItemId = R.id.bookingFragment
        }
    }
}
