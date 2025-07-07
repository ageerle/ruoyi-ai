/**
 * 工具日志显示组件
 * 负责显示工具执行的实时状态和结果
 */

class ToolLogDisplay {
    constructor(taskId) {
        this.taskId = taskId;
        this.toolCards = new Map(); // toolName -> DOM element
        this.container = this.createContainer();
        this.appendToPage();
    }

    // 创建容器
    createContainer() {
        const container = document.createElement('div');
        container.className = 'tool-log-container';
        container.id = `tool-log-${this.taskId}`;
        container.innerHTML = `
            <div class="tool-log-header">
                <span class="tool-log-title">🔧 工具执行日志</span>
                <span class="connection-status">连接中...</span>
            </div>
            <div class="tool-log-content">
                <!-- 工具卡片将在这里动态添加 -->
            </div>
        `;
        return container;
    }

    // 添加到页面
    appendToPage() {
        messagesContainer.appendChild(this.container);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // 显示连接状态
    showConnectionStatus(status) {
        const statusElement = this.container.querySelector('.connection-status');
        if (statusElement) {
            statusElement.textContent = status;
            statusElement.className = `connection-status ${status === '已连接' ? 'connected' : 'error'}`;
        }
    }

    // 添加工具开始执行
    addToolStart(logEvent) {
        // 移除等待状态卡片（如果存在）
        removeWaitingToolCard();
        
        const toolCard = this.createToolCard(logEvent);
        const content = this.container.querySelector('.tool-log-content');
        content.appendChild(toolCard);

        this.toolCards.set(logEvent.toolName, toolCard);
        this.scrollToBottom();
    }

    // 更新工具执行成功
    updateToolSuccess(logEvent) {
        const toolCard = this.toolCards.get(logEvent.toolName);
        if (toolCard) {
            this.updateToolCard(toolCard, logEvent, 'success');
        }
    }

    // 更新工具执行失败
    updateToolError(logEvent) {
        const toolCard = this.toolCards.get(logEvent.toolName);
        if (toolCard) {
            this.updateToolCard(toolCard, logEvent, 'error');
        }
    }

    // 创建工具卡片
    createToolCard(logEvent) {
        const card = document.createElement('div');
        card.className = 'tool-card running';
        card.innerHTML = `
            <div class="tool-header">
                <span class="tool-icon">${logEvent.icon}</span>
                <span class="tool-name">${logEvent.toolName}</span>
                <span class="tool-status">⏳ 执行中</span>
            </div>
            <div class="tool-file">📁 ${this.getFileName(logEvent.filePath)}</div>
            <div class="tool-message">${logEvent.message}</div>
            <div class="tool-time">开始时间: ${logEvent.timestamp}</div>
        `;
        return card;
    }

    // 更新工具卡片
    updateToolCard(toolCard, logEvent, status) {
        toolCard.className = `tool-card ${status}`;

        const statusElement = toolCard.querySelector('.tool-status');
        const messageElement = toolCard.querySelector('.tool-message');
        const timeElement = toolCard.querySelector('.tool-time');

        if (status === 'success') {
            statusElement.innerHTML = '✅ 完成';
            statusElement.className = 'tool-status success';
        } else if (status === 'error') {
            statusElement.innerHTML = '❌ 失败';
            statusElement.className = 'tool-status error';
        }

        messageElement.textContent = logEvent.message;

        if (logEvent.executionTime) {
            timeElement.textContent = `完成时间: ${logEvent.timestamp} (耗时: ${logEvent.executionTime}ms)`;
        }

        this.scrollToBottom();
    }

    // 显示任务完成
    showTaskComplete() {
        const header = this.container.querySelector('.tool-log-header');
        header.innerHTML = `
            <span class="tool-log-title">🎉 任务执行完成</span>
            <span class="connection-status completed">已完成</span>
        `;
    }

    // 淡出效果
    fadeOut() {
        this.container.style.transition = 'opacity 1s ease-out';
        this.container.style.opacity = '0.5';

        setTimeout(() => {
            if (this.container.parentNode) {
                this.container.parentNode.removeChild(this.container);
            }
        }, 10000); // 10秒后移除
    }

    // 滚动到底部
    scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // 获取文件名
    getFileName(filePath) {
        if (!filePath) return '未知文件';
        const parts = filePath.split(/[/\\]/);
        return parts[parts.length - 1] || filePath;
    }
}
