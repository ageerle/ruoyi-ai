# 使用 Python 3.8 slim 作为基础镜像（轻量稳定）
FROM python:3.10

# 设置工作目录
WORKDIR /app

# 复制所有文件到容器
COPY . /app

# 更新 pip 并安装 wheel（避免依赖问题）
RUN pip install --upgrade pip wheel
 
# 使用阿里云 PyPI 镜像安装依赖，并添加 trusted-host
RUN pip install -i https://mirrors.aliyun.com/pypi/simple \
    --trusted-host mirrors.aliyun.com \
    --no-cache-dir -r requirements.txt

# 暴露 Flask 端口
EXPOSE 5000

# 设置环境变量
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0

# 运行 Flask
CMD ["flask", "run", "--host=0.0.0.0"]
