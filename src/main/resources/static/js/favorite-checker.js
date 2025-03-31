//テーブルロールは一部更新されるため、DOMがリセットされeventlistenerがなくなる
//このためにテーブルボディは更新されないから、ボディにeventlistenerを追加
document.addEventListener("DOMContentLoaded", () => {
    // テーブルのボディを取得
    const stocksTB = document.querySelector("#stocksTB");

    // 子要素にevent追加
    stocksTB.addEventListener('click', (event) => {
        if (event.target && event.target.matches('.favoriteCheckbox')) {
            updateFavorite(event.target);
        }
    });
});

const updateFavorite = checkbox => {
    // checkbox的状態を取得
    const isFavorite = checkbox.checked;

    // 現在のticker_idを取得
    const tickerId = checkbox.getAttribute('data-ticker-id');
    console.log(isFavorite + " " + tickerId);

   //Content-Typeでpatch請求し、変数をバックにあげる
    fetch('/updateFavorites', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `isFavorite=${isFavorite}&tickerId=${tickerId}`
    })
    .then(response => {
        if (response.ok) {
            console.log("Successfully updated favorite status");
        } else {
            console.log("Failed to update favorite status");
        }
    })
    .catch(error => console.error('Error:', error));
};
