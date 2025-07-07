/**
 * SSE日志流管理器
 * 负责管理Server-Sent Events连接和工具日志显示
 */

// SSE实时日志管理器
class LogStreamManager {
    constructor() {
        this.activeConnections = new Map(); // taskId -> EventSource
        this.toolLogDisplays = new Map(); // taskId -> ToolLogDisplay
    }

    // 建立SSE连接
    startLogStream(taskId) {
        if (this.activeConnections.has(taskId)) {
            console.log('SSE连接已存在:', taskId);
            return;
        }

        console.log('🔗 建立SSE连接:', taskId);

        // 创建工具日志显示组件
        const toolLogDisplay = new ToolLogDisplay(taskId);
        this.toolLogDisplays.set(taskId, toolLogDisplay);

        // 建立EventSource连接
        const eventSource = new EventSource(`/api/logs/stream/${taskId}`);

        eventSource.onopen = () => {
            console.log('✅ SSE连接建立成功:', taskId);
            toolLogDisplay.showConnectionStatus('已连接');
        };

        eventSource.onmessage = (event) => {
            try {
                const logEvent = JSON.parse(event.data);
                console.log('📨 收到日志事件:', logEvent);
                this.handleLogEvent(taskId, logEvent);
            } catch (error) {
                console.error('解析日志事件失败:', error);
            }
        };

        // 监听特定的 "log" 事件
        eventSource.addEventListener('log', (event) => {
            try {
                const logEvent = JSON.parse(event.data);
                console.log('📨 收到log事件:', logEvent);
                this.handleLogEvent(taskId, logEvent);
            } catch (error) {
                console.error('解析log事件失败:', error);
            }
        });

        eventSource.onerror = (error) => {
            console.error('❌ SSE连接错误:', error);
            toolLogDisplay.showConnectionStatus('连接错误');
            this.handleConnectionError(taskId);
        };

        this.activeConnections.set(taskId, eventSource);
    }

    // 处理日志事件
    handleLogEvent(taskId, logEvent) {
        const toolLogDisplay = this.toolLogDisplays.get(taskId);
        if (!toolLogDisplay) {
            console.warn('找不到工具日志显示组件:', taskId);
            return;
        }

        switch (logEvent.type) {
            case 'CONNECTION_ESTABLISHED':
                toolLogDisplay.showConnectionStatus('已连接');
                // 连接建立后，如果5秒内没有工具事件，显示提示
                setTimeout(() => {
                    const waitingCard = document.getElementById('waiting-tool-card');
                    if (waitingCard) {
                        const waitingText = waitingCard.querySelector('.waiting-text');
                        if (waitingText) {
                            waitingText.textContent = '连接已建立，等待AI开始执行工具...';
                        }
                    }
                }, 5000);
                break;
            case 'TOOL_START':
                toolLogDisplay.addToolStart(logEvent);
                break;
            case 'TOOL_SUCCESS':
                toolLogDisplay.updateToolSuccess(logEvent);
                break;
            case 'TOOL_ERROR':
                toolLogDisplay.updateToolError(logEvent);
                break;
            case 'TASK_COMPLETE':
                toolLogDisplay.showTaskComplete();
                this.handleTaskComplete(taskId);
                this.closeConnection(taskId);
                break;
            default:
                console.log('未知日志事件类型:', logEvent.type);
        }
    }

    // 关闭SSE连接
    closeConnection(taskId) {
        const eventSource = this.activeConnections.get(taskId);
        if (eventSource) {
            eventSource.close();
            this.activeConnections.delete(taskId);
            console.log('🔚 关闭SSE连接:', taskId);
        }

        // 延迟移除显示组件
        setTimeout(() => {
            const toolLogDisplay = this.toolLogDisplays.get(taskId);
            if (toolLogDisplay) {
                toolLogDisplay.fadeOut();
                this.toolLogDisplays.delete(taskId);
            }
        }, 5000);
    }

    // 处理任务完成
    async handleTaskComplete(taskId) {
        try {
            // 获取对话结果
            const response = await fetch(`/api/task/result/${taskId}`);
            const resultData = await response.json();

            // 安全地显示最终结果
            if (typeof addMessage === 'function' && resultData && resultData.fullResponse) {
                addMessage('assistant', resultData.fullResponse);
            } else {
                console.error('addMessage function not available or invalid result data');
            }

            // 显示统计信息
            let statusMessage = '对话完成';
            if (resultData.totalTurns > 1) {
                statusMessage += ` (${resultData.totalTurns} 轮`;
                if (resultData.totalDurationMs) {
                    statusMessage += `, ${(resultData.totalDurationMs / 1000).toFixed(1)}秒`;
                }
                statusMessage += ')';

                if (resultData.reachedMaxTurns) {
                    statusMessage += ' - 达到最大轮次限制';
                }
                if (resultData.stopReason) {
                    statusMessage += ` - ${resultData.stopReason}`;
                }
            }

            // 安全地调用showStatus函数
            if (typeof showStatus === 'function') {
                showStatus(statusMessage, 'success');
            } else {
                console.log(statusMessage);
            }

        } catch (error) {
            console.error('获取对话结果失败:', error);
            // 安全地调用showStatus函数
            if (typeof showStatus === 'function') {
                showStatus('获取对话结果失败', 'error');
            } else {
                console.error('获取对话结果失败');
            }
        }
    }

    // 处理连接错误
    handleConnectionError(taskId) {
        // 可以实现重连逻辑
        console.log('处理连接错误:', taskId);
        setTimeout(() => {
            if (!this.activeConnections.has(taskId)) {
                console.log('尝试重连:', taskId);
                this.startLogStream(taskId);
            }
        }, 3000);
    }
}

// 创建SSE日志流管理器实例
const logStreamManager = new LogStreamManager();

// 确保在全局作用域中可用
window.logStreamManager = logStreamManager;
