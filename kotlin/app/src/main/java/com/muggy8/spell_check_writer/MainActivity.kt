package com.muggy8.spell_check_writer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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
    fun buildFilesMenu(filesDrawer: NavigationView){
        val sideBarMenu = filesDrawer.menu
        val filesListMenu = sideBarMenu.findItem(R.id.files_list).subMenu

        println("applicationInfo.dataDir: ${applicationInfo.dataDir}")
        directoryListing = DirectoryList(applicationInfo.dataDir)
        directoryListing.renderToMenu(filesListMenu)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        directoryListing.menuClicked(this, menuItem)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        permissionChecker.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}

class FilesListItem<T>(var name: String, var payload:T? = null) {
    var id: Int
    private var _type: String = "file"
    private var rng = Random()
    var type: String
        get() {
            return _type
        }
        set(newType) {
            if (newType != "file" && newType != "folder"){
                return
            }
            _type = newType
        }
    var onClick = fun(){}

    init {
        id = rng.nextInt()
    }

    fun renderToMenu(menu: Menu){
        val menuSize = menu.size()
        val menuItem = menu.add(
            1,
            id,
            menuSize,
            name
        )

        if (type === "folder"){
            menuItem.setIcon(R.drawable.ic_folder)
        }
        else {
            menuItem.setIcon(R.drawable.ic_file)
        }
    }

    fun removeFromMenu(menu: Menu){
        menu.removeItem(id)
    }
}

class DirectoryList(pathName: String = "") {
    private var contents = mutableListOf<FilesListItem<Path>>()
    private var path: Path

    init {
        path = Path(pathName)
        if (pathName !== ""){
            updatePath(pathName)
        }
    }

    constructor(path:Path):this(){
        updatePath(path)
    }

    fun updatePath(pathName:String){
        path = Path(pathName)
        updatePath(path)
    }

    fun updatePath(path: Path){
        if (!path.isDirectory()){
            throw Error("Path is not a directory")
        }
        val directoryContents = path.listDirectoryEntries()
        contents.clear()
        for (item in directoryContents){
            val listing = FilesListItem(item.name, item)
            contents.add(listing)

            if (item.isDirectory()){
                listing.type = "folder"
            }
            else{
                listing.type = "file"
            }
        }
    }

    fun renderToMenu(menu: Menu){
        while (!menu.isEmpty()){
            val forRemoval = menu.get(0)
            menu.removeItem(forRemoval.itemId)
        }

        for (item in contents){
            item.renderToMenu(menu)
        }
    }

    fun removeFromMenu(menu: Menu){
        for (item in contents){
            item.removeFromMenu(menu)
        }
    }

    fun menuClicked(context: Context, menuItem: MenuItem){
        val menuItemId = menuItem.itemId
        for(item in contents){
//            println("item.id: ${item.id} == menuItemId: ${menuItemId} => ${item.id == menuItemId}")
            if (item.id == menuItemId){
                if (item.onClick !== null){
                    item.onClick()
                }
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

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }
}