package com.muggy8.spell_check_writer

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.get
import androidx.core.view.isEmpty
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

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
    private var backFolderButton = FilesListItem()
    private var currentWorkingPath = Path("")

    init {
        backFolderButton.nameRes = R.string.back_folder
        backFolderButton.iconRes = R.drawable.ic_back_folder
        backFolderButton.onClick = fun(){
            updatePath(currentWorkingPath.parent)
            renderToMenu()
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
        currentWorkingPath = path
        if (canGoUp()){
            if(!renderedAboveDirectoryContents.contains(backFolderButton)){
                renderedAboveDirectoryContents.add(0, backFolderButton)
            }
        }
        else if (renderedAboveDirectoryContents.contains(backFolderButton)){
            renderedAboveDirectoryContents.remove(backFolderButton)
        }

        // draw all the contents of this folder
        this.directoryContents.clear()
        try{
            val directoryContents = path.listDirectoryEntries()
            for (item in directoryContents){
                val listing = FilesListItem(item.name)
                if (item.isDirectory()){
                    listing.iconRes = R.drawable.ic_folder
                    listing.onClick = fun(){
                        updatePath(item.pathString)
                        renderToMenu()
                    }
                }
                else{
                    listing.iconRes = R.drawable.ic_file
                    listing.onClick = fun(){
                        mainActivity.openFile(item)
                    }
                }

                this.directoryContents.add(listing)
            }
        }
        catch (e:Exception){
            // meh whatever we probably dont have permission so lets pretend it's an empty file
            val inaccessableListing = FilesListItem(R.string.contents_inaccessable)
            this.directoryContents.add(inaccessableListing)
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
            item.renderToMenu(menu)
        }
        for (item in renderedBelowDirectoryContents){
            item.renderToMenu(menu)
        }
    }

    fun renderToMenu(){
        Handler(Looper.getMainLooper()).postDelayed(
            fun(){
                if (menuPreviouslyRenderedTo == null){
                    return
                }
                renderToMenu(menuPreviouslyRenderedTo!!)
            },
            251
        )

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

    fun hasNoContents():Boolean{
        return directoryContents.isEmpty()
    }
    fun canGoUp():Boolean{
        val parentPath = currentWorkingPath.parent
        if (parentPath == null){
            return false
        }
        try {
            parentPath.listDirectoryEntries()
        }
        catch (e: Exception){
            return false
        }

        return true
    }

    fun getCurrentWorkingPath():Path{
        return currentWorkingPath
    }
}