# 使用官方 Python 作为基础镜像
FROM python:3.8-slim

# 设置工作目录为 /app
WORKDIR /app

# 复制当前目录下的所有文件到 Docker 容器的 /app 目录
COPY . /app

# 安装应用依赖
RUN pip install --no-cache-dir -r requirements.txt

# 暴露 Flask 应用使用的端口
EXPOSE 5000

# 设置环境变量
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0

# 启动 Flask 应用
CMD ["flask", "run", "--host=0.0.0.0"]
