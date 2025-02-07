# Redis 报错 Permission denied
- - -
### 此报错为无权限

需确保 redis 数据存储文件夹具有写权限

```shell
chmod 777 /docker/redis/data
```

没有写权限无法对数据进行存储

### 关于RDB报错 `/etc` 无权限问题

增加redis密码校验 无密码导致配置不安全
