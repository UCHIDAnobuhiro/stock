// dropdownButtonを押下時にmenuが表示される
document.getElementById("technicalDropdownButton").addEventListener("click", () => {
	const menu = document.getElementById("technicalDropdownMenu");
	menu.classList.toggle("hidden");
});

// dropdownButtonとmenuではないものを押下時にmenuを隠す
document.addEventListener("click", (event) => {
	const menu = document.getElementById("technicalDropdownMenu");
	const button = document.getElementById("technicalDropdownButton");
	if (!menu.contains(event.target) && event.target !== button) {
		//classにhiddenを追加
		menu.classList.add("hidden");
	}
});
