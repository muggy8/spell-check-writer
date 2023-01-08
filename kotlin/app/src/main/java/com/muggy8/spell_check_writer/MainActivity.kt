package com.muggy8.spell_check_writer

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.SubMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var primaryToolbar:Toolbar
    private lateinit var filesDrawer:NavigationView
    private lateinit var filesListToggle:ActionBarDrawerToggle
    private lateinit var mainAppView: DrawerLayout
    private val permissionChecker: PermssionController = PermssionController(this)
    lateinit var filesListMenu: SubMenu
    private lateinit var directoryListing: DirectoryList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filesDrawer = findViewById(R.id.files_drawer_container)
        mainAppView = findViewById(R.id.main_appview)
        primaryToolbar = findViewById(R.id.primary_toolbar)
        setSupportActionBar(primaryToolbar)

        filesListToggle = ActionBarDrawerToggle(
            this,
            mainAppView,
            primaryToolbar,
            R.string.open_files_drawer,
            R.string.close_files_drawer,
        )

        mainAppView.addDrawerListener(filesListToggle)
        filesListToggle.syncState()

        filesDrawer.setNavigationItemSelectedListener(this)

        val sideBarMenu = filesDrawer.menu
        filesListMenu = sideBarMenu.findItem(R.id.files_list).subMenu

        if (savedInstanceState === null){
            directoryListing = DirectoryList(this)
            if (permissionChecker.hasFileAccessPermission()){
                directoryListing.updatePath(Environment.getExternalStorageDirectory().absolutePath)
            }
            renderMenu()
        }
    }

    override fun onBackPressed() {
        if (mainAppView.isDrawerOpen(GravityCompat.START)){
            return mainAppView.closeDrawer(GravityCompat.START)
        }

        super.onBackPressed()
    }

    private lateinit var requestForStoragePermissionButton:FilesListItem

    private fun rebuildPermissionRequester(){
        println("building open folder button")
        if (permissionChecker.hasFileAccessPermission()){
            if (::requestForStoragePermissionButton.isInitialized){
                directoryListing.renderedBelowDirectoryContents.remove(
                    requestForStoragePermissionButton
                )
            }
        }
        else {
            if (::requestForStoragePermissionButton.isInitialized){
                requestForStoragePermissionButton.nameRes = permissionChecker.requestPermissionLabelTextRes()
            }
            else{
                requestForStoragePermissionButton = FilesListItem(
                    permissionChecker.requestPermissionLabelTextRes()
                )
                requestForStoragePermissionButton.onClick = fun (){
                    permissionChecker.requestFileAccessPermission()
                }

                directoryListing.renderedBelowDirectoryContents.add(
                    requestForStoragePermissionButton
                )
            }
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        directoryListing.menuClicked(menuItem)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        renderMenu()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus){
            if (permissionChecker.hasFileAccessPermission() && directoryListing.hasNoContents()){
                directoryListing.updatePath(Environment.getExternalStorageDirectory().absolutePath)
            }
            renderMenu()
        }
        super.onWindowFocusChanged(hasFocus)
    }

    private fun renderMenu(){
        if (! ::filesListMenu.isInitialized){
            return
        }
        rebuildPermissionRequester()
        directoryListing.renderToMenu(filesListMenu)
    }
}