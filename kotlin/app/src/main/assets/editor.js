function loadFile(filePath, fileData64){
    console.log(filePath, fileData64)
    let tabsList = document.getElementById("tabs-list")
    let editor = document.getElementById("editor")
    let tab = document.getElementById(filePath)
    if (tab){
        return tab.click()
    }

    let loadData = ()=>{
        editor.innerText = atob(fileData64)

        setTimeout(()=>editor.scrollTop = editor.scrollHeight, 50)
    }

    tab = document.createElement("div")
    let pathParts = filePath.split("/")
    tab.textContent = pathParts[pathParts.length - 1]
    tab.id = filePath
    tab.onClick = loadData

    loadData()
}
