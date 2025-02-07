# 关于vue与boot整合部署
- - -
* [前端静态资源如何整合到后端访问](https://doc.ruoyi.vip/ruoyi-vue/other/faq.html#前端静态资源如何整合到后端访问)

3.X 需在 `pom.xml` 增加资源过滤排除

```xml
<resource>
    <directory>src/main/resources/页面目录</directory>
    <!-- 关闭过滤 -->
    <filtering>false</filtering>
</resource>
```
