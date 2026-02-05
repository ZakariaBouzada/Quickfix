package com.example.bikerepairapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerepairapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

class MechanicActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mechanic)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mechanic_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.mechanicBottomNav)
        bottomNav.setupWithNavController(navController)
    }
}
