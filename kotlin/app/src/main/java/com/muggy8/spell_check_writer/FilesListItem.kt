package com.muggy8.spell_check_writer

import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.get
import androidx.core.view.isEmpty
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
            if (item.isDirectory()){
                listing.iconRes = R.drawable.ic_folder
            }
            else{
                listing.iconRes = R.drawable.ic_file
            }

            this.directoryContents.add(listing)
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