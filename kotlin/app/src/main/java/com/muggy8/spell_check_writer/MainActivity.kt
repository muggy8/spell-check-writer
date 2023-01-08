package com.muggy8.spell_check_writer

import android.net.Uri
import android.os.Bundle
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

        if (savedInstanceState === null){
            buildFilesMenu(filesDrawer)
        }
    }

    override fun onBackPressed() {
        if (mainAppView.isDrawerOpen(GravityCompat.START)){
            return mainAppView.closeDrawer(GravityCompat.START)
        }

        super.onBackPressed()
    }

    private lateinit var directoryListing: DirectoryList
    private lateinit var filesListMenu: SubMenu
    private fun buildFilesMenu(filesDrawer: NavigationView){
        val sideBarMenu = filesDrawer.menu
        filesListMenu = sideBarMenu.findItem(R.id.files_list).subMenu

        println("applicationInfo.dataDir: ${applicationInfo.dataDir}")
        directoryListing = DirectoryList(applicationInfo.dataDir)

        val folderAdd = FilesListItem(R.string.add_folder)
        folderAdd.iconRes = R.drawable.ic_folder_add
        directoryListing.renderedBelowDirectoryContents.add(
            folderAdd
        )

        val fileAdd = FilesListItem(R.string.add_file)
        fileAdd.iconRes = R.drawable.ic_file_add
        directoryListing.renderedBelowDirectoryContents.add(
            fileAdd
        )

        rebuildOpenFolder()

        directoryListing.renderToMenu(filesListMenu)
    }

    private lateinit var openFolderButton:FilesListItem
    private lateinit var requestForStoragePermissionButton:FilesListItem
    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){
        directoryListing.updatePathFromFiletreeUri(this, it)
    }

    private fun rebuildOpenFolder(){
        println("building open folder button")
        if (permissionChecker.hasFileAccessPermission()){
            if (! ::openFolderButton.isInitialized){
                openFolderButton = FilesListItem(R.string.open_folder)
                directoryListing.renderedBelowDirectoryContents.add(
                    openFolderButton
                )
                openFolderButton.onClick = fun(){
                    pickFolder.launch(Uri.EMPTY)
                }
            }
            if (::requestForStoragePermissionButton.isInitialized){
                directoryListing.renderedBelowDirectoryContents.remove(
                    requestForStoragePermissionButton
                )
            }
        }
        else {
            if (::openFolderButton.isInitialized){
                directoryListing.renderedBelowDirectoryContents.remove(
                    openFolderButton
                )
            }
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
        rebuildOpenFolder()
        directoryListing.renderToMenu(filesListMenu)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus){
            rebuildOpenFolder()
            directoryListing.renderToMenu(filesListMenu)
        }
        super.onWindowFocusChanged(hasFocus)
    }
}