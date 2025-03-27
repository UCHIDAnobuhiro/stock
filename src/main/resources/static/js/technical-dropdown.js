// dropdownButtonを押下時にmenuが表示される
document.getElementById("technicalDropdownButton").addEventListener("click",  ()=> {
    let menu = document.getElementById("technicalDropdownMenu");
    menu.classList.toggle("hidden");
});

// dropdownButtonとmenuではないものを押下時にmenuを隠す
document.addEventListener("click",  (event)=> {
    let menu = document.getElementById("technicalDropdownMenu");
    let button = document.getElementById("technicalDropdownButton");
    if (!menu.contains(event.target) && event.target !== button) {
		//classにhiddenを追加
        menu.classList.add("hidden");
    }
});
