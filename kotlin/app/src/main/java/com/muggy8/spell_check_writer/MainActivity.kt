package com.muggy8.spell_check_writer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.util.Random

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var primaryToolbar:Toolbar
    private lateinit var filesDrawer:NavigationView
    private lateinit var filesListToggle:ActionBarDrawerToggle
    private lateinit var mainAppView: DrawerLayout

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

        buildFilesMenu(filesDrawer)
    }

    override fun onBackPressed() {
        if (mainAppView.isDrawerOpen(GravityCompat.START)){
            return mainAppView.closeDrawer(GravityCompat.START)
        }

        super.onBackPressed()
    }

    fun buildFilesMenu(filesDrawer: NavigationView){
        val sideBarMenu = filesDrawer.menu
        val filesListMenu = sideBarMenu.findItem(R.id.files_list).subMenu
        val placeholderTest = FilesListItem("Testing test 123")
        placeholderTest.renderToMenu(filesListMenu)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        return true
    }
}

class FilesListItem {
    var id: Int
    private var _type: String = "file"
    private var rng = Random()
    var type: String
        get() {
            return _type
        }
        set(newType:String) {
            if (newType !== "file" || newType !== "folder"){
                return
            }
            _type = newType
        }
    var name: String
    var onClick = fun(){}

    constructor(name:String) {
        id = rng.nextInt()
        this.name = name
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