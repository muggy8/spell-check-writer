package com.muggy8.spell_check_writer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.nio.file.Path
import java.util.Random
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

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
        directoryListing.renderedBelowDirectoryContents.add(
            FilesListItem(R.string.add_folder)
        )
        directoryListing.renderedBelowDirectoryContents.add(
            FilesListItem(R.string.add_file)
        )

        buildOpenFolder()

        directoryListing.renderToMenu(filesListMenu)
    }

    private lateinit var openFolderButton:FilesListItem
    private lateinit var requestForStoragePermissionButton:FilesListItem
    private fun buildOpenFolder(){
        println("building open folder buton")
        if (permissionChecker.hasFileAccessPermission()){
            if (! ::openFolderButton.isInitialized){
                openFolderButton = FilesListItem(R.string.open_folder)
                directoryListing.renderedBelowDirectoryContents.add(
                    openFolderButton
                )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        permissionChecker.onActivityResult(requestCode, resultCode, data)
        buildOpenFolder()
        directoryListing.renderToMenu(filesListMenu)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults)
        buildOpenFolder()
        directoryListing.renderToMenu(filesListMenu)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

class FilesListItem() {
    var id: Int
    private var rng = Random()
    var onClick = fun(){}

    init {
        id = rng.nextInt()
    }

    var name:String? = null
    var nameRes:Int? = null
    constructor(name:String):this(){
        this.name = name
    }
    constructor(nameRes:Int):this(){
        this.nameRes = nameRes
    }

    var icon:Drawable? = null
    var iconRes:Int? = null
    fun renderToMenu(menu: Menu){
        val menuSize = menu.size()
        var menuItem:MenuItem? = null
        if (name != null){
            menuItem = menu.add(
                1,
                id,
                menuSize,
                name as String
            )
        }
        else if (nameRes != null){
            menuItem = menu.add(
                1,
                id,
                menuSize,
                nameRes as Int
            )
        }
        if (menuItem != null){
            if (iconRes != null){
                menuItem.setIcon(iconRes as Int)
            }
            if (iconRes != null){
                menuItem.setIcon(icon)
            }
        }
    }

    fun removeFromMenu(menu: Menu){
        menu.removeItem(id)
    }
}

class DirectoryList(private var path: Path = Path("")) {
    private var directoryContents = mutableListOf<FilesListItem>()
    var renderedAboveDirectoryContents = mutableListOf<FilesListItem>()
    var renderedBelowDirectoryContents = mutableListOf<FilesListItem>()

    init {
        updatePath(path)
    }

    constructor(pathName:String):this(Path(pathName))

    fun updatePath(pathName:String){
        path = Path(pathName)
        updatePath(path)
    }

    fun updatePath(path: Path){
        if (!path.isDirectory()){
            throw Error("Path is not a directory")
        }
        val directoryContents = path.listDirectoryEntries()
        this.directoryContents.clear()
        for (item in directoryContents){
            val listing = FilesListItem(item.name)
            this.directoryContents.add(listing)

            if (item.isDirectory()){
                listing.iconRes = R.drawable.ic_folder
            }
            else{
                listing.iconRes = R.drawable.ic_file
            }
        }
    }

    fun renderToMenu(menu: Menu){
        clearMenu(menu)
        for (item in renderedAboveDirectoryContents){
            item.renderToMenu(menu)
        }
        for (item in directoryContents){
            item.renderToMenu(menu)
        }
        for (item in renderedBelowDirectoryContents){
            item.renderToMenu(menu)
        }
    }

    fun clearMenu(menu: Menu){
        while (!menu.isEmpty()){
            val forRemoval = menu.get(0)
            menu.removeItem(forRemoval.itemId)
        }
    }

    fun removeFromMenu(menu: Menu){
        for (item in directoryContents){
            item.removeFromMenu(menu)
        }
    }

    fun menuClicked(menuItem: MenuItem){
        val menuItemId = menuItem.itemId
        var allReneredItems = mutableListOf<FilesListItem>()
        allReneredItems.addAll(directoryContents)
        allReneredItems.addAll(renderedBelowDirectoryContents)
        allReneredItems.addAll(renderedAboveDirectoryContents)
        for(item in allReneredItems){
//            println("item.id: ${item.id} == menuItemId: ${menuItemId} => ${item.id == menuItemId}")
            if (item.id == menuItemId){
                item.onClick()
                return
            }
        }
    }
}

class PermssionController (val context: AppCompatActivity) {
    private val missingPermissions = mutableListOf<String>()
    private val RECORD_REQUEST_CODE = 1337 // i have no idea what this is for but it seems like it should be constant

    fun hasFileAccessPermission():Boolean{
        var hasPermission = true
        missingPermissions.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasManageExternalStoragePermission = Environment.isExternalStorageManager()
            hasPermission = hasPermission && hasManageExternalStoragePermission
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val hasReadPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasReadPermission){
                missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            hasPermission = hasPermission && hasReadPermission
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasWritePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasWritePermission){
                missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            hasPermission = hasPermission && hasWritePermission
        }

        return hasPermission
    }

    fun requestFileAccessPermission(){
        hasFileAccessPermission()
        if (!missingPermissions.isEmpty()){
            ActivityCompat.requestPermissions(
                context,
                missingPermissions.toTypedArray(),
                RECORD_REQUEST_CODE,
            )
        }
        else {
            try {
                val intentToGetManageStoragePermission = Intent(
                    ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                )
                context.startActivity(intentToGetManageStoragePermission)
            }
            catch(e: Exception){
                val intentToGetManageStoragePermission = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intentToGetManageStoragePermission)
            }
        }
    }


    fun requestPermissionLabelTextRes():Int{
        if (hasFileAccessPermission()){
            throw Error("Permission already granted")
        }
        if (missingPermissions.isEmpty()){
            return R.string.grant_manage_storage_permission
        }
        return R.string.grant_storage_permission
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

    }
}