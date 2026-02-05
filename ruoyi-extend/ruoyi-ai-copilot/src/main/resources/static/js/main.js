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
window.addEventListener('error', function (event) {
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
        // 直接处理流式响应
        handleStreamResponse(message);
        showStatus('开始流式对话...', 'success');
    } catch (error) {
        console.error('Error:', error);
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
        await fetch('/api/chat/clear', {method: 'POST'});
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

// 解析SSE格式的数据 (data: content)
function parseSseData(line) {
    const trimmedLine = line.trim();
    if (!trimmedLine) return null;

    // SSE格式: data: content
    if (trimmedLine.startsWith('data:')) {
        return trimmedLine.substring(5).trim(); // 去掉 "data:" 前缀
    }

    return null;
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

    fetch('/api/chat/message', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({message: userMessage})
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            // 获取响应体的ReadableStream
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = ''; // 用于缓存不完整的数据

            // 处理流式数据
            const processStream = () => {
                return reader.read().then(({ done, value }) => {
                    if (done) {
                        console.log('✅ 流式响应完成');
                        // 处理剩余的缓存数据
                        if (buffer.trim()) {
                            const remainingLines = buffer.split('\n');
                            for (const line of remainingLines) {
                                const content = parseSseData(line);
                                if (content && content !== '[DONE]') {
                                    fullContent += content;
                                }
                            }
                        }
                        // 移除加载指示器
                        streamIndicator.remove();
                        // 格式化最终内容
                        streamContent.innerHTML = formatMessage(fullContent);
                        return;
                    }

                    // 解码数据块并添加到缓存
                    buffer += decoder.decode(value, { stream: true });
                    console.log('📨 收到数据块，缓存内容:', buffer);

                    // 按行处理数据（SSE格式为逐行）
                    const lines = buffer.split('\n');

                    // 最后一行可能不完整，保留在缓存中
                    buffer = lines.pop() || '';

                    for (const line of lines) {
                        // 解析SSE格式的数据 (data: content)
                        const content = parseSseData(line);

                        if (!content) continue;

                        // 检查是否是完成标记
                        if (content === '[DONE]') {
                            console.log('✅ 收到完成信号');
                            streamIndicator.remove();
                            streamContent.innerHTML = formatMessage(fullContent);
                            return Promise.resolve();
                        }

                        // 添加内容到全局变量
                        fullContent += content;

                        // 实时更新UI（删除之前的加载指示器并显示内容）
                        if (streamIndicator.parentNode) {
                            streamIndicator.style.display = 'none';
                        }
                        streamContent.innerHTML = formatMessage(fullContent);
                    }

                    // 滚动到最新位置
                    messagesContainer.scrollTop = messagesContainer.scrollHeight;

                    // 继续读取下一个数据块
                    return processStream();
                });
            };

            return processStream();
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
messageInput.addEventListener('keypress', function (e) {
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
window.addEventListener('load', function () {
    messageInput.focus();

    // 确保函数在全局作用域中可用
    window.addMessage = addMessage;
    window.showStatus = showStatus;

    // 调试信息
    debugVariables();
});
