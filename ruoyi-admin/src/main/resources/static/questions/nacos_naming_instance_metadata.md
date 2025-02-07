# nacos 报错 The Raft Group [naming_instance_metadata]
- - -
## Nacos 服务下线报错问题

问题描述：

Nacos 服务管理 > 服务列表 > 详情 > 下线 报错



报错详情：

```
caused: errCode: 500, errMsg: do metadata operation failed ;caused: com.alibaba.nacos.consistency.exception.ConsistencyException: The Raft Group [naming_instance_metadata] did not find the Leader node;caused: The Raft Group [naming_instance_metadata] did not find the Leader node;
```



解决方案：

**删除 Nacos 根目录下 data 文件夹下的 protocol 文件夹**

（推荐使用全局搜索软件查询，windows 环境根目录一般在 C:\Users\用户名\nacos）



问题原因：

> Nacos 采用 raft 算法来计算 Leader，并且会记录上次启动的集群地址，所以当我们自己的服务器 IP 改变时(网络环境不稳定，如WIFI， IP 地址也经常变化)，导致 raft 记录的集群地址失效，导致选 Leader 出现问题。



参考目录：

[解决疑难问题之服务下线报：The Raft Group naming_instance_metadata\] did not find the Leader node; - 嘉美祥瑞 - 博客园 (cnblogs.com)](https://www.cnblogs.com/whl-jx911/p/16736625.html)