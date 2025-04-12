# Sharding-Proxy搭建分库分表
- - -

# 如何使用

查看 `ruoyi-demo` 服务 `TestShardingController`

![输入图片说明](https://foruda.gitee.com/images/1688014028842337522/cd26026a_1766278.png "屏幕截图")

## 首先在 MySQL 创建两个库

创建两个库 `data-center_0` `data-center_1` 分别执行如下SQL

```sql
CREATE TABLE `t_order_0` (
  `order_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `total_money` int(10) UNSIGNED NOT NULL COMMENT '订单总金额',
  PRIMARY KEY (`order_id`),
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单总表';
 
CREATE TABLE `t_order_1` (
  `order_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `total_money` int(10) UNSIGNED NOT NULL COMMENT '订单总金额',
  PRIMARY KEY (`order_id`),
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单总表';
 
CREATE TABLE `t_order_item_0` (
  `order_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '子订单ID',
  `order_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `money` int(10) UNSIGNED NOT NULL COMMENT '子订单金额',
  PRIMARY KEY (`order_item_id`),
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单子表';
 
CREATE TABLE `t_order_item_1` (
  `order_item_id` bigint(20) UNSIGNED NOT NULL COMMENT '子订单ID',
  `order_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户ID',
  `money` int(10) UNSIGNED NOT NULL COMMENT '子订单金额',
  PRIMARY KEY (`order_item_id`),
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单子表';
 
```

## 然后更改配置文件

更改 `config-sharding.yaml` 配置文件内的数据库连接地址与用户名密码

## 服务搭建

参考部署文档上传 docker 文件夹 内部包含 `shardingproxy` 配置文件

![输入图片说明](https://foruda.gitee.com/images/1688013921062151295/89652dda_1766278.png "屏幕截图")

框架已经包含了 docker-compose 编排 执行如下命令启动容器即可

```shell
docker-compose up -d shardingproxy
```

## 最后运行 demo

运行 demo 提供的 controller 代码查看数据库内数据即可

## 用法参考视频(略有不同 理性观看)

用法参考视频: https://www.bilibili.com/video/BV1XN411A7Tv/
