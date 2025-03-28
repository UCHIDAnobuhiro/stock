function updateFavorite(checkbox) {
    // 获取checkbox的选中状态
    const isFavorite = checkbox.checked;

    // 获取对应的ticker.id
    const tickerId = checkbox.getAttribute('data-ticker-id');
    console.log(isFavorite + " " + tickerId);

    // 发送PATCH请求到后端
    fetch('/updateFavorites', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'isFavorite=' + isFavorite + '&tickerId=' + tickerId
    })
    .then(response => {
        // 因为不关心响应内容，所以我们只检查响应状态
        if (response.ok) {
            console.log("Successfully updated favorite status");
        } else {
            console.log("Failed to update favorite status");
        }
    })
    .catch(error => console.error('Error:', error));
}
