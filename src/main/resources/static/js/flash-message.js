window.addEventListener('DOMContentLoaded', () => {
	const flashElement = document.getElementById('flash-message');
	if (flashElement) {
		const message = flashElement.dataset.message;
		if (message) {
			alert(message);
		}
	}
});