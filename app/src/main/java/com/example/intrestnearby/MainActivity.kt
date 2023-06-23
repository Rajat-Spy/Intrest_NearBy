package com.example.intrestnearby

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.intrestnearby.databinding.ActivityMainBinding
import com.example.intrestnearby.databinding.NavigationDrawerLayoutBinding
import com.example.intrestnearby.databinding.ToolbarLayoutBinding

class MainActivity : AppCompatActivity() {
    private lateinit var navigationDrawerLayoutBinding: NavigationDrawerLayoutBinding
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var toolbarLayoutBinding: ToolbarLayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationDrawerLayoutBinding = NavigationDrawerLayoutBinding.inflate(layoutInflater)
        setContentView(navigationDrawerLayoutBinding.root)
        mainBinding = navigationDrawerLayoutBinding.mainActivity
        toolbarLayoutBinding = mainBinding.toolbarMain
        setSupportActionBar(toolbarLayoutBinding.toolbar)

        val toggle = ActionBarDrawerToggle(this, navigationDrawerLayoutBinding.navDrawer,
            toolbarLayoutBinding.toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer)
        navigationDrawerLayoutBinding.navDrawer.addDrawerListener(toggle)
        toggle.syncState()
        val navController = Navigation.findNavController(this, R.id.fragmentContainer)
        NavigationUI.setupWithNavController(
            navigationDrawerLayoutBinding.navigationView, navController
        )
        val headerLayout = navigationDrawerLayoutBinding.navigationView.getHeaderView(0)
    }

    override fun onBackPressed() {
        if(navigationDrawerLayoutBinding.navDrawer.isDrawerOpen(GravityCompat.START))
            navigationDrawerLayoutBinding.navDrawer.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }
}