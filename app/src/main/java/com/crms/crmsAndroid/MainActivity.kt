package com.crms.crmsAndroid

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.crms.crmsAndroid.databinding.ActivityMainBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.TriggerDownFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var objRfidScanner: rfidScanner
        private set;
    lateinit var navController:NavController
        private set
    init {
        System.loadLibrary("IGLBarDecoder")
        System.loadLibrary("IGLImageAE")
        try {
            objRfidScanner = rfidScanner()
        } catch (
            e: Exception
        ) {
            Log.e("E", "can not init objRfidScanner", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_manInventory,
                R.id.nav_gallery,
                R.id.nav_slideshow,
                R.id.nav_login,
                R.id.searchItem,
                R.id.nav_testRfid
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        objRfidScanner.free()
        super.onDestroy()
    }
    fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    ?.childFragmentManager
                    ?.fragments
                    ?.firstOrNull { it.isVisible } // Get the visible fragment
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 139 280 293
        if (keyCode == 280) {
            if (event.repeatCount == 0) {
                val currentFragment = getCurrentFragment()
                if (currentFragment != null && currentFragment is TriggerDownFragment) {
                    currentFragment.onTriggerDown()
                }
            }
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    // 9 DO BY DANTEH

}