package com.matejdro.wearmusiccenter.view.mainactivity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.matejdro.wearmusiccenter.NotificationService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearmusiccenter.config.WatchInfoWithIcons
import com.matejdro.wearmusiccenter.databinding.ActivityMainBinding
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import com.matejdro.wearmusiccenter.view.FabFragment
import com.matejdro.wearmusiccenter.view.MiscSettingsFragment
import com.matejdro.wearmusiccenter.view.TitledActivity
import com.matejdro.wearmusiccenter.view.actionlist.ActionListFragment
import com.matejdro.wearmusiccenter.view.buttonconfig.ButtonConfigFragment
import com.matejdro.wearutils.companionnotice.WearCompanionPhoneActivity


class MainActivity : WearCompanionPhoneActivity(), NavigationView.OnNavigationItemSelectedListener,
        ConfigActivityComponentProvider, TitledActivity, ActivityResultReceiver {
    private lateinit var viewmodel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProviders.of(this)[MainActivityViewModel::class.java]

        viewmodel.watchInfoProvider.observe(this, watchInfoObserver)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerToggle = ActionBarDrawerToggle(
                this, binding.drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(drawerToggle)

        binding.navView.setNavigationItemSelectedListener(this)

        binding.appBar?.fab?.setOnClickListener {
            val currentFragment = currentFragment ?: return@setOnClickListener
            if (currentFragment is FabFragment) {
                currentFragment.onFabClicked()
            }
        }

        updateCurrentFragment(supportFragmentManager.findFragmentById(R.id.fragment_container))
    }

    override fun onResume() {
        super.onResume()

        showNotificationServiceWarning()
    }

    private fun disableDrawer() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerToggle.onDrawerStateChanged(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerToggle.isDrawerIndicatorEnabled = false
        drawerToggle.syncState()
    }

    private fun enableDrawer() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        drawerToggle.onDrawerStateChanged(DrawerLayout.LOCK_MODE_UNLOCKED)
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerToggle.syncState()
    }

    private val watchInfoObserver = Observer<WatchInfoWithIcons> {
        if (it != null) {
            enableDrawer()

            if (currentFragment == null || currentFragment is NoWatchFragment) {
                swapFragment(ButtonConfigFragment.newInstance(true))
                binding.navView.menu.getItem(0).isChecked = true

            }
        } else {
            disableDrawer()
            swapFragment(NoWatchFragment())
        }
    }

    private fun swapFragment(newFragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .commit()

        updateCurrentFragment(newFragment)
    }


    private fun updateCurrentFragment(newFragment: Fragment?) {
        currentFragment = newFragment

        if (newFragment == null) {
            return
        }

        if (newFragment is FabFragment) {
            binding.appBar?.fab?.let {
                it.show()
                newFragment.prepareFab(it)
            }
        } else {
            binding.appBar?.fab?.hide()
        }
    }

    private fun showNotificationServiceWarning() {
        if (NotificationService.isEnabled(this)) {
            return
        }

        val builder = AlertDialog.Builder(this)

        builder
                .setTitle(getString(R.string.error_service_not_enabled))
                .setNegativeButton(android.R.string.cancel, null)
                .setMessage(getString(R.string.error_service_not_enabled_description))
                .setPositiveButton(getString(R.string.action_open_settings),
                        { _, _ ->
                            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        })

        builder.show()
    }


    override fun onBackPressed() {
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val currentFragment = currentFragment
        if (currentFragment is Fragment) {
            currentFragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.playing_controls -> {
                swapFragment(ButtonConfigFragment.newInstance(true))
            }
            R.id.stopped_controls -> {
                swapFragment(ButtonConfigFragment.newInstance(false))
            }
            R.id.actions_menu -> {
                swapFragment(ActionListFragment())
            }
            R.id.settings -> {
                swapFragment(MiscSettingsFragment())
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun provideConfigActivityComponent() = viewmodel.configActivityComponent

    override fun updateActivityTitle(newTitle: String) {
        supportActionBar!!.title = newTitle
    }

    override fun getWatchAppPresenceCapability(): String = CommPaths.WATCH_APP_CAPABILITY
}
