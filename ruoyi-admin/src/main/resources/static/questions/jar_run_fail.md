# 打包jar运行报错问题
- - -

**常见于 windows 平台以命令方式启动**

windows 平台默认编码为 GBK 所以读取到所有的配置都是乱码

## 解决方案

需要在命令增加 `-Dfile.encoding=utf-8` 指定文件编码

例如: `java -Dfile.encoding=utf-8 -jar ruoyi-xxx.jar`