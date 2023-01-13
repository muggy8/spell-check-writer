function loadFile(filePath, fileData64){
    let tabsList = document.getElementById("tabs-list")
    let editor = document.getElementById("editor")
    let tab = document.getElementById(filePath)
    let saveButton = document.getElementById("save-button")
    if (tab){
        return tab.click()
    }

    let saveData = ()=>{
        Android.saveFile(filePath, editor.innerText)
    }

    let loadData = ()=>{
        saveButton.classList.remove("hidden")
        saveButton.onclick = saveData
        editor.innerText = atob(fileData64)
        editor.setAttribute("contentEditable", true)
        setTimeout(()=>editor.scrollTop = editor.scrollHeight, 50)
    }

    tab = document.createElement("div")
    let pathParts = filePath.split("/")
    tab.textContent = pathParts[pathParts.length - 1]
    tab.id = filePath
    tab.onClick = loadData

    loadData()
}
