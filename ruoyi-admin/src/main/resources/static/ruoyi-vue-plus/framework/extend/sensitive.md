# 数据脱敏
- - -
## 功能说明

系统使用 `Jackson` 序列化策略 对标注了 `Sensitive` 注解的属性进行脱敏处理

## 使用教程

> 使用注解标注需要脱敏的字段 选择对应的策略

![输入图片说明](https://foruda.gitee.com/images/1699523591703893602/ffd6dba2_1766278.png "屏幕截图")

* strategy 脱敏策略
* roleKey 角色code(判断用户是否拥有角色权限)
* perms 权限code(判断用户是否拥有标识符权限)

![输入图片说明](https://foruda.gitee.com/images/1678979315796014155/614adf91_1766278.png "屏幕截图")

> 可再 `SensitiveStrategy` 内自定义策略

![输入图片说明](https://foruda.gitee.com/images/1678979319996224858/3b3e3c8b_1766278.png "屏幕截图")

## 脱敏逻辑修改

> 系统使用通用接口处理是否需要脱敏 多个系统可以自定义不同的脱敏逻辑实现

![输入图片说明](https://foruda.gitee.com/images/1678979325448998856/b262e425_1766278.png "屏幕截图")

> 系统默认处理逻辑为 根据角色与标识符或非管理员脱敏 可自行修改默认实现

![输入图片说明](https://foruda.gitee.com/images/1699523752627488891/f82f2f50_1766278.png "屏幕截图")


