package com.muggy8.spell_check_writer

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import java.nio.file.Path
import kotlin.io.path.pathString


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var primaryToolbar:Toolbar
    private lateinit var filesDrawer:NavigationView
    private lateinit var filesListToggle:ActionBarDrawerToggle
    private lateinit var mainAppView: DrawerLayout
    private val permissionChecker: PermssionController = PermssionController(this)
    lateinit var filesListMenu: SubMenu
    private lateinit var directoryListing: DirectoryList
    private lateinit var textInputArea: TextInputEditText
    val setCurrentAsDefaultButton: FilesListItem = FilesListItem()
    private lateinit var storage:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // stuff to deal with local storage
        storage = getSharedPreferences(
            getString(R.string.key_preference_file_key),
            MODE_PRIVATE
        )

        // stuff to do with the action bar and the drawer
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

        // stuff to do with the contents of the drawer
        val sideBarMenu = filesDrawer.menu
        filesListMenu = sideBarMenu.findItem(R.id.files_list).subMenu
        setCurrentAsDefaultButton.nameRes = R.string.current_as_default
        setCurrentAsDefaultButton.onClick = fun(){
            setDefaultFolder(directoryListing.getCurrentWorkingPath())
        }

        // stuff to do with the big text area where the text will be edited
        textInputArea = findViewById(R.id.edit_text_area)
        textInputArea.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged( text: CharSequence, start: Int, count: Int, after: Int ){}
            override fun onTextChanged( text: CharSequence, start: Int, before: Int, count: Int){}
        })

        // initiate some states only if we're not starting for he first time
        if (savedInstanceState === null){
            directoryListing = DirectoryList(this)
            if (permissionChecker.hasFileAccessPermission()){
                val defaultPath = storage.getString(
                    getString(R.string.key_preference_file_key),
                    Environment.getExternalStorageDirectory().absolutePath
                )
                if (defaultPath != null){
                    directoryListing.updatePath(defaultPath)
                }
                else{
                    directoryListing.updatePath(Environment.getExternalStorageDirectory().absolutePath)
                }
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
            if (!directoryListing.renderedBelowDirectoryContents.contains(setCurrentAsDefaultButton)){
                directoryListing.renderedBelowDirectoryContents.add(setCurrentAsDefaultButton)
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
                val defaultPath = storage.getString(
                    getString(R.string.key_preference_file_key),
                    Environment.getExternalStorageDirectory().absolutePath
                )
                if (defaultPath != null){
                    directoryListing.updatePath(defaultPath)
                }
                else{
                    directoryListing.updatePath(Environment.getExternalStorageDirectory().absolutePath)
                }
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

    private fun setDefaultFolder(path: Path){
        val storageEditor = storage.edit()
        storageEditor.putString(getString(R.string.key_preference_file_key), path.toAbsolutePath().pathString)
        storageEditor.apply()
    }

    fun openFile(filePath: Path){

        val file = filePath.toFile()
        val fileContents = file.readText()

        textInputArea.setText(fileContents)
        textInputArea.text?.let { textInputArea.setSelection(it.length) }
        if (mainAppView.isDrawerOpen(GravityCompat.START)){
            return mainAppView.closeDrawer(GravityCompat.START)
        }
    }
}

