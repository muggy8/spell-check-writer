package com.muggy8.spell_check_writer

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.TypedValue
import android.view.MenuItem
import android.view.SubMenu
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.charset.Charset
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
    val openFolderButton: FilesListItem = FilesListItem()
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
        openFolderButton.nameRes = R.string.open_folder
        openFolderButton.onClick = fun(){
            selectFolder()
        }

        // stuff to do with the big text area where the text will be edited
        editorWebapp = findViewById(R.id.editing_webapp)
        editorWebapp.settings.javaScriptEnabled = true
        editorWebapp.settings.domStorageEnabled = true
        editorWebapp.addJavascriptInterface(this, "Android")

        val backgroundColor = TypedValue()
        val color = TypedValue()
        val colorPrimary = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, backgroundColor, true)
        theme.resolveAttribute(android.R.attr.colorForeground, color, true)
        theme.resolveAttribute(android.R.attr.colorPrimary, colorPrimary, true)

        val editorFile = application.assets.open("editor.html")
        var editorHTML = editorFile.bufferedReader()
            .use(BufferedReader::readText)
        editorFile.close()

        editorWebapp.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // time to use the super archaic method of loading in js because the normal way wont work for some reason >:(
                val jsFile = application.assets.open("editor.js")
                val jsContents = jsFile.bufferedReader()
                    .use(BufferedReader::readText)
                jsFile.close()

                editorWebapp.evaluateJavascript(jsContents){}
            }
        })

        editorHTML = editorHTML.replace("/*theme-replacement*/", """
            html, body {
                --color: ${color.coerceToString().removeRange(1, 3)};
                --background-color: ${backgroundColor.coerceToString().removeRange(1, 3)};
                --color-primary: ${colorPrimary.coerceToString().removeRange(1, 3)};
                background-color: var(--background-color);
                color: var(--color);
            }
        """.trimIndent())

        val encodedHtml = Base64.encodeToString(editorHTML.toByteArray(), Base64.NO_PADDING)
        editorWebapp.loadData(encodedHtml, "text/html", "base64")

        // initiate some states only if we're not starting for he first time
        if (savedInstanceState === null){
            directoryListing = DirectoryList(this)
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

    private val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            // call this to persist permission across decice reboots
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            // do your stuff

//            setDefaultFolder(uri)

            directoryListing.updatePath(it)
        }
    }

    private fun selectFolder(){
        dirRequest.launch(Uri.parse(Environment.getExternalStorageDirectory().toString()))
    }

    private fun rebuildPermissionRequester(){
        println("building open folder button")
        if (permissionChecker.hasFileAccessPermission()){
            if (::requestForStoragePermissionButton.isInitialized){
                directoryListing.renderedBelowDirectoryContents.remove(
                    requestForStoragePermissionButton
                )
            }
            if (!directoryListing.renderedBelowDirectoryContents.contains(openFolderButton)){
                directoryListing.renderedBelowDirectoryContents.add(openFolderButton)
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

    val openedFiles = HashMap<String, DocumentFile>()
    fun openFile(file: DocumentFile){
        val inputStream = contentResolver.openInputStream(file.uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val contents = reader.use(BufferedReader::readText)
        val encodedFileContents = Base64.encodeToString(contents.toByteArray(), Base64.NO_PADDING)
        val filePath = file.uri.toString()
        openedFiles.set(filePath, file)
        val jsCode = """loadFile(`${filePath}`, `${encodedFileContents}`)"""

        editorWebapp.evaluateJavascript( jsCode ) {
            mainAppView.closeDrawer(GravityCompat.START)
        }
    }

    @JavascriptInterface
    fun saveFile(id: String, contents: String){
        println(contents)
        println(id)

        val file = openedFiles.get(id)
        println(file)
        if (file != null) {

            val outputStream = contentResolver.openOutputStream(file.uri, "rwt")
            if (outputStream != null) {
                val writer = outputStream.writer(Charset.defaultCharset())
                writer.write(contents)
                writer.flush()
                writer.close()
            }
        }
    }

}

fun getRandomString(length: Int) : String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { charset.random() }
        .joinToString("")
}