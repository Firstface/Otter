package com.example.otter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.example.otter.databinding.ActivityMainBinding
import com.example.otter.ui.HomeFragment
import com.example.otter.ui.ProfileFragment
import com.example.otter.ui.RecommendationFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 1. Create and hold fragment instances
    private val homeFragment by lazy { HomeFragment() }
    private val recommendationFragment by lazy { RecommendationFragment() }
    private val profileFragment by lazy { ProfileFragment() }
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Add all fragments initially, hide all but the first
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, profileFragment, "3").hide(profileFragment)
                .add(R.id.fragment_container, recommendationFragment, "2").hide(recommendationFragment)
                .add(R.id.fragment_container, homeFragment, "1")
                .commit()
        }

        // 3. Set up the listener to show/hide fragments
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> switchFragment(homeFragment)
                R.id.navigation_recommend -> switchFragment(recommendationFragment)
                R.id.navigation_profile -> switchFragment(profileFragment)
            }
            true
        }
    }

    // 4. New function to show/hide fragments
    private fun switchFragment(targetFragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment)
        transaction.show(targetFragment)
        transaction.commit()
        activeFragment = targetFragment
    }
}