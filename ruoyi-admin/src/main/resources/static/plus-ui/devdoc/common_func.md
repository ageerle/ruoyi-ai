# 通用方法
- - -

### $tab对象
> `$tab`对象用于做页签操作、刷新页签、关闭页签、打开页签、修改页签等，它定义在`plugins/tab.ts`文件中，它有如下方法
* 打开页签

```typescript
// 打开页签
proxy?.$tab.openPage('/system/user');
// 打开页签并指定页签标题
proxy?.$tab.openPage('/system/user', '用户管理');
proxy?.$tab.openPage('/system/user', '用户管理').then(() => {
    // 执行结束的逻辑
})
```

* 修改页签

```typescript
// 修改当前页签
const obj = Object.assign({}, route, { title: '自定义标题' });
proxy?.$tab.updatePage(obj);
```
* 关闭页签

```typescript
// 关闭当前
proxy?.$tab.closePage();
// 关闭指定页签
const obj = { path: "/system/user", name: "User" };
proxy?.$tab.closePage(obj);

proxy?.$tab.closePage(obj).then(() => {
    // 执行结束的逻辑
})
```

* 刷新页签

```typescript
// 刷新当前页签
proxy?.$tab.refreshPage();

// 刷新指定页签
const obj = { path: "/system/user", name: "User" };
proxy?.$tab.refreshPage(obj);

proxy?.$tab.refreshPage(obj).then(() => {
    // 执行结束的逻辑
})
```

* 关闭所有页签

```typescript
proxy?.$tab.closeAllPage();

proxy?.$tab.closeAllPage().then(() => {
  // 执行结束的逻辑
})
```

* 关闭左侧页签

```typescript
// 关闭当前页签的左侧页签
proxy?.$tab.closeLeftPage();

// 关闭指定页签的左侧页签
const obj = { path: "/system/user", name: "User" };
proxy?.$tab.closeLeftPage(obj);

proxy?.$tab.closeLeftPage(obj).then(() => {
  // 执行结束的逻辑
})
```

* 关闭右侧页签

```typescript
// 关闭当前页签的右侧页签
proxy?.$tab.closeRightPage();

// 关闭指定页签的右侧页签
const obj = { path: "/system/user", name: "User" };
proxy?.$tab.closeRightPage(obj);

proxy?.$tab.closeRightPage(obj).then(() => {
  // 执行结束的逻辑
})
```

* 关闭其他页签

```typescript
proxy?.$tab.closeOtherPage();

const obj = { path: "/system/user", name: "User" };
proxy?.$tab.closeOtherPage(obj);

proxy?.$tab.closeOtherPage(obj).then(() => {
  // 执行结束的逻辑
})
```

### $modal对象
> `$modal`对象用于做消息提示、通知提示、对话框提醒、二次确认、遮罩等，它定义在`plugins/modal.ts`文件中，它有如下方法

* 提供成功、警告和错误等反馈信息

```typescript
proxy?.$modal.msg("默认反馈");
proxy?.$modal.msgError("错误反馈");
proxy?.$modal.msgSuccess("成功反馈");
proxy?.$modal.msgWarning("警告反馈");
```

* 提供成功、警告和错误等提示信息

```typescript
proxy?.$modal.alert("默认提示");
proxy?.$modal.alertError("错误提示");
proxy?.$modal.alertSuccess("成功提示");
proxy?.$modal.alertWarning("警告提示");
```

* 提供成功、警告和错误等通知信息

```typescript
proxy?.$modal.notify("默认通知");
proxy?.$modal.notifyError("错误通知");
proxy?.$modal.notifySuccess("成功通知");
proxy?.$modal.notifyWarning("警告通知");
```

* 提供确认窗体信息

```typescript
proxy?.$modal.confirm('确认信息').then(function() {
  ...
}).then(() => {
  ...
}).catch(() => {});
```

* 提供遮罩层信息

```typescript
// 打开遮罩层
proxy?.$modal.loading("正在导出数据，请稍后...");

// 关闭遮罩层
proxy?.$modal.closeLoading();
```

### $auth对象
> `$auth`对象用于验证用户是否拥有某（些）权限或角色，它定义在`plugins/auth.ts`文件中，它有如下方法

* 验证用户权限

```typescript
// 验证用户是否具备某权限
proxy?.$auth.hasPermi("system:user:add");
// 验证用户是否含有指定权限，只需包含其中一个
proxy?.$auth.hasPermiOr(["system:user:add", "system:user:update"]);
// 验证用户是否含有指定权限，必须全部拥有
proxy?.$auth.hasPermiAnd(["system:user:add", "system:user:update"]);
```

* 验证用户角色

```typescript
// 验证用户是否具备某角色
proxy?.$auth.hasRole("admin");
// 验证用户是否含有指定角色，只需包含其中一个
proxy?.$auth.hasRoleOr(["admin", "common"]);
// 验证用户是否含有指定角色，必须全部拥有
proxy?.$auth.hasRoleAnd(["admin", "common"]);
```

### $cache对象
> `$cache`对象用于处理缓存。我们并不建议您直接使用`sessionStorage`或`localStorage`(vue3版本推荐使用useStorage)，因为项目的缓存策略可能发生变化，通过`$cache`对象做一层调用代理则是一个不错的选择。`$cache`提供`session`和`local`两种级别的缓存，如下：

| 对象名称 | 缓存类型                           |
| -------- | ---------------------------------- |
| session  | 会话级缓存，通过sessionStorage实现 |
| local    | 本地级缓存，通过localStorage实现   |


**示例**

```typescript
// local 普通值
proxy?.$cache.local.set('key', 'local value')
console.log(proxy?.$cache.local.get('key')) // 输出'local value'

// session 普通值
proxy?.$cache.session.set('key', 'session value')
console.log(proxy?.$cache.session.get('key')) // 输出'session value'

// local JSON值
proxy?.$cache.local.setJSON('jsonKey', { localProp: 1 })
console.log(proxy?.$cache.local.getJSON('jsonKey')) // 输出'{localProp: 1}'

// session JSON值
proxy?.$cache.session.setJSON('jsonKey', { sessionProp: 1 })
console.log(proxy?.$cache.session.getJSON('jsonKey')) // 输出'{sessionProp: 1}'

// 删除值
proxy?.$cache.local.remove('key')
proxy?.$cache.session.remove('key')
```

### $download对象

> `$download`对象用于文件下载，它定义在`plugins/download.ts`文件中，它有如下方法

* 通过ossId从存储中下载文件

``` typescript
// 默认下载方法
proxy?.$download.oss(ossId);
```

* 根据请求地址下载zip包

```typescript
const url = '/tool/gen/batchGenCode?tables=' + tableNames;
const name = 'ruoyi';

// 默认方法
proxy?.$download.zip(url, name);
```
