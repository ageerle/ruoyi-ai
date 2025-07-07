/**
 * SpringAI Alibaba Copilot - 主JavaScript文件
 * 处理聊天界面交互、SSE连接和工具日志显示
 */

// 全局变量
const messagesContainer = document.getElementById('messages');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const clearBtn = document.getElementById('clearBtn');
const loading = document.getElementById('loading');
const status = document.getElementById('status');

// 全局错误处理
window.addEventListener('error', function(event) {
    console.error('Global JavaScript error:', event.error);
    if (event.error && event.error.message && event.error.message.includes('userMessage')) {
        console.error('Detected userMessage error, this might be a variable scope issue');
    }
});

// 函数声明会被提升，但为了安全起见，我们在页面加载后再设置全局引用

// 发送消息
async function sendMessage() {
    const message = messageInput.value.trim();
    if (!message) return;

    // 添加用户消息
    addMessage('user', message);
    messageInput.value = '';

    // 显示加载状态
    showLoading(true);
    setButtonsEnabled(false);

    try {
        const response = await fetch('/api/chat/message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message: message })
        });

        const data = await response.json();

        if (data.success) {
            // 如果是异步任务（工具调用），建立SSE连接
            if (data.taskId && data.asyncTask) {
                // 先显示等待状态的工具卡片
                showWaitingToolCard();
                logStreamManager.startLogStream(data.taskId);
                showStatus('任务已启动，正在建立实时连接...', 'success');
            } else if (data.streamResponse) {
                // 流式对话响应
                handleStreamResponse(message);
                showStatus('开始流式对话...', 'success');
            } else {
                // 同步任务，直接显示结果
                addMessage('assistant', data.message);

                // 显示连续对话统计信息
                let statusMessage = 'Message sent successfully';
                if (data.totalTurns && data.totalTurns > 1) {
                    statusMessage += ` (${data.totalTurns} turns`;
                    if (data.totalDurationMs) {
                        statusMessage += `, ${(data.totalDurationMs / 1000).toFixed(1)}s`;
                    }
                    statusMessage += ')';

                    if (data.reachedMaxTurns) {
                        statusMessage += ' - Reached max turns limit';
                    }
                    if (data.stopReason) {
                        statusMessage += ` - ${data.stopReason}`;
                    }
                }
                showStatus(statusMessage, 'success');
            }
        } else {
            addMessage('assistant', data.message);
            showStatus('Error: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        // 更安全的错误处理
        const errorMessage = error && error.message ? error.message : 'Unknown error occurred';
        addMessage('assistant', 'Sorry, there was an error processing your request: ' + errorMessage);
        showStatus('Network error: ' + errorMessage, 'error');
    } finally {
        showLoading(false);
        setButtonsEnabled(true);
        messageInput.focus();
    }
}

// 快速操作
function quickAction(message) {
    messageInput.value = message;
    sendMessage();
}

// 清除历史
async function clearHistory() {
    try {
        await fetch('/api/chat/clear', { method: 'POST' });
        messagesContainer.innerHTML = '';
        showStatus('History cleared', 'success');
    } catch (error) {
        showStatus('Error clearing history', 'error');
    }
}

// 添加消息到界面
function addMessage(role, content) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;

    // 处理代码块和格式化
    const formattedContent = formatMessage(content);

    messageDiv.innerHTML = `
        <div>
            <div class="message-role">${role === 'user' ? 'You' : 'Assistant'}</div>
            <div class="message-content">${formattedContent}</div>
        </div>
    `;

    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// 格式化消息内容
function formatMessage(content) {
    // 简单的代码块处理
    content = content.replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');

    // 处理行内代码
    content = content.replace(/`([^`]+)`/g, '<code style="background: #f0f0f0; padding: 2px 4px; border-radius: 3px;">$1</code>');

    // 处理换行
    content = content.replace(/\n/g, '<br>');

    return content;
}

// 显示/隐藏加载状态
function showLoading(show) {
    loading.classList.toggle('show', show);
}

// 启用/禁用按钮
function setButtonsEnabled(enabled) {
    sendBtn.disabled = !enabled;
    clearBtn.disabled = !enabled;
}

// 显示状态消息
function showStatus(message, type) {
    status.textContent = message;
    status.className = `status ${type}`;
    status.style.display = 'block';

    setTimeout(() => {
        status.style.display = 'none';
    }, 3000);
}

// 显示等待状态的工具卡片
function showWaitingToolCard() {
    const waitingCard = document.createElement('div');
    waitingCard.className = 'tool-log-container waiting';
    waitingCard.id = 'waiting-tool-card';
    waitingCard.innerHTML = `
        <div class="tool-log-header">
            <span class="tool-log-title">🔧 工具执行准备中</span>
            <span class="connection-status connecting">连接中...</span>
        </div>
        <div class="tool-log-content">
            <div class="waiting-message">
                <div class="loading-spinner"></div>
                <div class="waiting-text">正在等待工具执行推送...</div>
                <div class="waiting-hint">AI正在分析您的请求并准备执行相应的工具操作</div>
            </div>
        </div>
    `;

    messagesContainer.appendChild(waitingCard);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// 处理流式响应
function handleStreamResponse(userMessage) {
    console.log('🌊 开始处理流式响应，消息:', userMessage);

    // 参数验证
    if (!userMessage) {
        console.error('handleStreamResponse: userMessage is undefined or empty');
        showStatus('流式响应参数错误', 'error');
        return;
    }

    // 创建流式消息容器
    const streamMessageId = 'stream-message-' + Date.now();
    const streamContainer = document.createElement('div');
    streamContainer.className = 'message assistant streaming';
    streamContainer.id = streamMessageId;
    streamContainer.innerHTML = `
        <div class="message-content">
            <div class="stream-content"></div>
            <div class="stream-indicator">
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    `;

    messagesContainer.appendChild(streamContainer);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    // 使用fetch API处理流式响应
    const streamContent = streamContainer.querySelector('.stream-content');
    const streamIndicator = streamContainer.querySelector('.stream-indicator');
    let fullContent = '';

    fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message: userMessage })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        function readStream() {
            return reader.read().then(({ done, value }) => {
                if (done) {
                    console.log('✅ 流式响应完成');
                    streamIndicator.style.display = 'none';
                    streamContainer.classList.remove('streaming');
                    showStatus('流式对话完成', 'success');
                    return;
                }

                const chunk = decoder.decode(value, { stream: true });
                console.log('📨 收到流式数据块:', chunk);

                // 处理SSE格式的数据
                const lines = chunk.split('\n');
                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const data = line.substring(6);
                        if (data === '[DONE]') {
                            console.log('✅ 流式响应完成');
                            streamIndicator.style.display = 'none';
                            streamContainer.classList.remove('streaming');
                            showStatus('流式对话完成', 'success');
                            return;
                        }

                        // 追加内容
                        fullContent += data;
                        streamContent.textContent = fullContent;
                        messagesContainer.scrollTop = messagesContainer.scrollHeight;
                    }
                }

                return readStream();
            });
        }

        return readStream();
    })
    .catch(error => {
        console.error('❌ 流式响应错误:', error);
        const errorMessage = error && error.message ? error.message : 'Unknown stream error';
        streamIndicator.innerHTML = '<span class="error">连接错误: ' + errorMessage + '</span>';
        showStatus('流式对话连接错误: ' + errorMessage, 'error');
    });
}

// 移除等待状态卡片
function removeWaitingToolCard() {
    const waitingCard = document.getElementById('waiting-tool-card');
    if (waitingCard) {
        waitingCard.remove();
    }
}

// 事件监听器
messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

// 调试函数
function debugVariables() {
    console.log('=== Debug Variables ===');
    console.log('messagesContainer:', messagesContainer);
    console.log('messageInput:', messageInput);
    console.log('sendBtn:', sendBtn);
    console.log('clearBtn:', clearBtn);
    console.log('loading:', loading);
    console.log('status:', status);
    console.log('addMessage function:', typeof addMessage);
    console.log('showStatus function:', typeof showStatus);
    console.log('logStreamManager:', typeof logStreamManager);
}

// 页面加载完成后聚焦输入框
window.addEventListener('load', function() {
    messageInput.focus();

    // 确保函数在全局作用域中可用
    window.addMessage = addMessage;
    window.showStatus = showStatus;

    // 调试信息
    debugVariables();
});
