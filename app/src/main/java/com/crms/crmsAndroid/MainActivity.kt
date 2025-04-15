package com.crms.crmsAndroid

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.databinding.ActivityMainBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.fyp.crms_backend.utils.AccessPagePermission
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var objRfidScanner: rfidScanner
        private set
    lateinit var navController: NavController
        private set
    private var isLongPress: Boolean = false

    private lateinit var sharedViewModel: SharedViewModel


    init {
        System.loadLibrary("IGLBarDecoder")
        System.loadLibrary("IGLImageAE")
        try {
            objRfidScanner = rfidScanner()
        } catch (e: Exception) {
            Log.e("E", "can not init objRfidScanner", e)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // 定義頂層導航目標
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_manInventory,
                R.id.nav_login,
                R.id.nav_inventory,
                R.id.nav_updateItem,
                R.id.nav_addItem,
                R.id.nav_deleteItem,
                R.id.nav_updateLoc,
                R.id.nav_newRoom,
                R.id.searchItem,
                //R.id.nav_testRfid
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        RetrofitClient.initialize(sharedViewModel)

        // 監聽權限變化並更新側邊欄
        sharedViewModel.accessPage.observe(this) { accessPage ->
            setupNavigationMenu(accessPage ?: 0) // 處理空值
        }

        // 初始權限檢查
        setupNavigationMenu(sharedViewModel.accessPage.value ?: 0)

        // 單一導航監聽器（合併邏輯）
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 控制 ActionBar 返回按鈕
            supportActionBar?.setDisplayHomeAsUpEnabled(destination.id != R.id.nav_login)

            // 權限檢查
            AccessPagePermission.fragmentPermissions[destination.id]?.let { permission ->
                if (!AccessPagePermission.hasPermission(
                        sharedViewModel.accessPage.value ?: 0,
                        permission
                    )
                ) {
                    Snackbar.make(binding.root, "permission denied", Snackbar.LENGTH_SHORT).show()
                    navController.navigate(R.id.searchItem) // 跳轉到默認允許的頁面
                }
            }
            invalidateOptionsMenu()
        }
    }

    // 根據權限設置側邊欄菜單可見性
// MainActivity.kt
    private fun setupNavigationMenu(accessPage: Int) {
        val navView: NavigationView = binding.navView
        navView.menu.forEach { menuItem ->
            val permission = AccessPagePermission.fragmentPermissions[menuItem.itemId]
            menuItem.isVisible = if (permission != null) {
                AccessPagePermission.hasPermission(accessPage, permission)
            } else {
                true // 未列出的默認可見
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (navController.currentDestination?.id == R.id.nav_login) {
            menu.findItem(R.id.action_settings)?.isVisible = false
            menu.findItem(R.id.action_logout)?.isVisible = false
        } else {
            menu.findItem(R.id.action_settings)?.isVisible = true
            menu.findItem(R.id.action_logout)?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
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
            val currentFragment = getCurrentFragment()
            if (currentFragment != null && currentFragment is ITriggerLongPress && event.repeatCount >= 1) {
                if (event.repeatCount == 1) {
                    isLongPress = true
                }
                if (isLongPress) {
                    currentFragment.onTriggerLongPress()
                }
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == 280) {
            val currentFragment = getCurrentFragment()
            if (currentFragment != null) {
                if (currentFragment is ITriggerLongPress && isLongPress) {
                    currentFragment.onTriggerRelease()
                } else if (currentFragment is ITriggerDown && !isLongPress) {
                    currentFragment.onTriggerDown()
                }
            }
            isLongPress = false
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_settings)
                true
            }

            R.id.action_logout -> {
                handleLogout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleLogout() {
        sharedViewModel.logout()
        setupNavigationMenu(0) // 重置菜單可見性
        navController.navigate(R.id.nav_login)
    }

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
    }

    override fun onResume() {
        super.onResume()
        objRfidScanner.setMode()
    }

}