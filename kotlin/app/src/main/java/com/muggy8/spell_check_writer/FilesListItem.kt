package com.muggy8.spell_check_writer

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.documentfile.provider.DocumentFile
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

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

    var icon: Drawable? = null
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
            else if (icon != null){
                menuItem.setIcon(icon)
            }
        }
    }

    fun removeFromMenu(menu: Menu){
        menu.removeItem(id)
    }
}

class DirectoryList(private var mainActivity: MainActivity) {
    private var directoryContents = mutableListOf<FilesListItem>()
    var renderedAboveDirectoryContents = mutableListOf<FilesListItem>()
    var renderedBelowDirectoryContents = mutableListOf<FilesListItem>()
    private val historicStateRenderer = mutableListOf<()->Unit>()
    private var backFolderButton = FilesListItem()

    init {
        backFolderButton.nameRes = R.string.back_folder
        backFolderButton.iconRes = R.drawable.ic_back_folder
        backFolderButton.onClick = fun(){
            if (historicStateRenderer.isEmpty()){
                return
            }

            val currentStateRenderer = historicStateRenderer.last()
            historicStateRenderer.remove(currentStateRenderer)

            val previousStateRenderer = historicStateRenderer.last()
            historicStateRenderer.remove(previousStateRenderer)
            previousStateRenderer()
            renderToMenu()
        }
    }

    fun updatePathFromFiletreeUri(uri: Uri){
        println("rendering uri: ${uri}")
        // manage the back button
        if (historicStateRenderer.size > 0){
            renderedAboveDirectoryContents.add(0, backFolderButton)
        }
        else if (renderedAboveDirectoryContents.isNotEmpty()){
            renderedAboveDirectoryContents.remove(backFolderButton)
        }

        historicStateRenderer.add(fun() {
            updatePathFromFiletreeUri(uri)
        })

        // actually figure out what the heck is in the folder and render it
        val documentFile = DocumentFile.fromTreeUri(mainActivity, uri)
        val directoryContents = documentFile!!.listFiles()
        this.directoryContents.clear()
        for (item in directoryContents){

            val listing = FilesListItem()
            listing.name = item.name
            if (item.isDirectory()){
                listing.iconRes = R.drawable.ic_folder
                listing.onClick = fun (){

                }
            }
            else{
                listing.iconRes = R.drawable.ic_file
            }

            this.directoryContents.add(listing)
        }
    }

    fun updatePath(pathName:String){
        val path = Path(pathName)
        updatePath(path)
    }

    fun updatePath(path: Path){
        if (!path.isDirectory()){
            throw Error("Path is not a directory")
        }

        println("rendering path: ${path}")
        // manage the back button
        if (historicStateRenderer.size > 0){
            renderedAboveDirectoryContents.add(0, backFolderButton)
        }
        else if (renderedAboveDirectoryContents.isNotEmpty()){
            renderedAboveDirectoryContents.remove(backFolderButton)
        }

        historicStateRenderer.add(fun() {
            println("reverting to state located at ${path}")
            updatePath(path.toString())
        })

        // draw all the contents of this folder
        val directoryContents = path.listDirectoryEntries()
        this.directoryContents.clear()
        for (item in directoryContents){
            val listing = FilesListItem(item.name)
            if (item.isDirectory()){
                listing.iconRes = R.drawable.ic_folder
            }
            else{
                listing.iconRes = R.drawable.ic_file
            }

            this.directoryContents.add(listing)
        }
    }

    var menuPreviouslyRenderedTo:Menu? = null
    fun renderToMenu(menu: Menu){
        if (menuPreviouslyRenderedTo != menu){
            menuPreviouslyRenderedTo = menu
        }
        clearMenu(menu)
        for (item in renderedAboveDirectoryContents){
            item.renderToMenu(menu)
        }
        for (item in directoryContents){
            println("adding to menu: ${item.name}")
            item.renderToMenu(menu)
        }
        for (item in renderedBelowDirectoryContents){
            item.renderToMenu(menu)
        }
    }

    fun renderToMenu(){
        mainActivity.renderMenu()
    }

    fun clearMenu(menu: Menu){
        println("cleaning menu")

        while (!menu.isEmpty()){
            val forRemoval = menu.get(0)
            menu.removeItem(forRemoval.itemId)
            println("forRemoval: ${forRemoval}")
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