package com.muggy8.spell_check_writer

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.TypedValue
import android.view.MenuItem
import android.view.SubMenu
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.io.BufferedReader
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
    val setCurrentAsDefaultButton: FilesListItem = FilesListItem()
    private lateinit var storage:SharedPreferences
    private lateinit var editorWebapp: WebView

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
        editorWebapp = findViewById(R.id.editing_webapp)
        editorWebapp.settings.javaScriptEnabled = true
        editorWebapp.addJavascriptInterface(this, "Android")
        application.assets.open("editor.html").apply{
//            var editorHTML = this.readBytes()
            var editorHTML = this.bufferedReader()
                .use(BufferedReader::readText)
            val encodedHtml = Base64.encodeToString(editorHTML.toByteArray(), Base64.NO_PADDING)
            editorWebapp.loadData(encodedHtml, "text/html", "base64")
        }.close()

        val colorPrimary = TypedValue()
        val white = TypedValue()
        val black = TypedValue()
        val theme: Theme = this.getTheme()
        theme.resolveAttribute(R.color.black, black, true)
        theme.resolveAttribute(R.color.white, white, true)


        println("attribute value")
        println(white.coerceToString())
        println(black.coerceToString())
        println(ContextCompat.getColor(this, R.color.black))


        var backgroundColor = white
        var color = black

        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                backgroundColor = black
                color = white
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                backgroundColor = white
                color = black
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                backgroundColor = white
                color = black
            }
        }
        editorWebapp.evaluateJavascript("""
            (function(){
                function convertHex(hexCode, opacity = 1){
                    let hex = hexCode.replace('#', '');
                
                    let r,g,b,
                        a = "F"
                    switch(hex.length){
                        case 3: [r,g,b] = hex
                            
                            break
                        case 4: [r,g,b,a] = hex
                            break
                        case 8: 
                            a = hex.substring(6,8)
                        case 6: 
                            r = hex.substring(0,2)
                            g = hex.substring(2,4)
                            b = hex.substring(4,6)
                            break
                    }
                    
                    r = parseInt(r)
                    g = parseInt(g)
                    b = parseInt(b)
                    a = parseInt(a)
                    
                    return [r,g,b,a]
                }
                
                function subtractFromColor(colorToSubtractFrom, colorToSubtract){
                    let subFrom = convertHex(colorToSubtractFrom)
                    let sub = convertHex(colorToSubtract)
                    
                    let [r,g,b,a] = subFrom.map((sourceColorVal, index)=>sourceColorVal - sub[index])
                    
                    return "rgba(r,g,b,a)"
                        .replace("r", r)
                        .replace("g", g)
                        .replace("b", b)
                        .replace("a", a)
                }
            
                document.body.style.backgroundColor = "${backgroundColor.coerceToString()}";
                document.body.style.color = "${color.coerceToString()}"
                
            })()
        """.trimIndent()){

        }

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


    }

    @JavascriptInterface
    fun saveFile(path: String, contents: String){}
}

