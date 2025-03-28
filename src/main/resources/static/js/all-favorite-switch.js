document.addEventListener("DOMContentLoaded", () => {
	const buttons = document.querySelectorAll('input[name="showButton"]');
	let isFetching = false; // 重複リクエスト防止

	buttons.forEach(button => {
		button.addEventListener('change', async (event) => {
			if (isFetching) return; // リクエストしているならreturn

			isFetching = true; // リクエスト中　制限加える
			const selectedValue = event.target.value;
			try {
				const response = await fetch(`/stock?show=${selectedValue}`, {
					method: 'PATCH'
				});
				if (!response.ok) throw new Error("リクエスト失敗");
				
				//fargment「fragments/stock/stock-show.html :: stocksDetailsTR」を取得
				const html = await response.text(); 
				
				//stocksTBは更新されるfargmentを引用しているため、再引用することで、一部更新ができる
				document.querySelector("#stocksTB").innerHTML = html;
			} catch (error) {
				console.error("Error:", error);
			} finally {
				isFetching = false;//リクエスト完了(finally：失敗成功問わず)　制限解除
			}
		});
	});
});
