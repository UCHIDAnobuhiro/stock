document.addEventListener('DOMContentLoaded', () => {
    const trendLineTools = document.querySelectorAll('input[name="trendLineTool"]');
    
    trendLineTools.forEach(input => {
        input.addEventListener('change', (event) => {
            const currentInput = event.target;
            if (currentInput.checked) {
                // 取消选择其他工具
                trendLineTools.forEach(tool => {
                    if (tool !== currentInput) {
                        tool.checked = false;
                    }
                });
            }
        });
    });
});