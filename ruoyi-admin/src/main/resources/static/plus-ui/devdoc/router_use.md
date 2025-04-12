# 路由使用
- - -

框架的核心是通过路由自动生成对应导航，所以除了路由的基本配置，还需要了解框架提供了哪些配置项。
### 路由配置
```typescript
// 当设置 true 的时候该路由不会在侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
hidden: true // (默认 false)

//当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
redirect: 'noRedirect'

// 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
// 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
// 若你想不管路由下面的 children 声明的个数都显示你的根路由
// 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
alwaysShow: true

name: 'router-name' // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
query: '{"id": 1, "name": "ry"}'     // 访问路由的默认传递参数
roles: ['admin', 'common']           // 访问路由的角色权限
permissions: ['a:a:a', 'b:b:b']      // 访问路由的菜单权限
 
meta: {
  title: 'title' // 设置该路由在侧边栏和面包屑中展示的名字
  icon: 'svg-name' // 设置该路由的图标，支持 svg-class，也支持 el-icon-x element-ui 的 icon
  noCache: true // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
  breadcrumb: false //  如果设置为false，则不会在breadcrumb面包屑中显示(默认 true)
  affix: true // 如果设置为true，它则会固定在tags-view中(默认 false)

  // 当路由设置了该属性，则会高亮相对应的侧边栏。
  // 这在某些场景非常有用，比如：一个文章的列表页路由为：/article/list
  // 点击文章进入文章详情页，这时候路由为/article/1，但你想在侧边栏高亮文章列表的路由，就可以进行如下设置
  activeMenu: '/article/list'
}
```
**普通示例**
```json
{
  path: '/system/test',
  component: Layout,
  redirect: 'noRedirect',
  hidden: false,
  alwaysShow: true,
  meta: { title: '系统管理', icon : "system" },
  children: [{
    path: 'index',
    component: (resolve) => require(['@/views/index'], resolve),
    name: 'Test',
    meta: {
      title: '测试管理',
      icon: 'user'
    }
  }]
}
```
**外链示例**
```json
{
  path: 'http://ruoyi.vip',
  meta: { title: '若依官网', icon : "guide" }
}
```
### 静态路由
代表那些不需要动态判断权限的路由，如登录页、404、等通用页面，在`@/router/index.ts`配置对应的公共路由。
### 动态路由
代表那些需要根据用户动态判断权限并通过addRoutes动态添加的页面，在`@/store/modules/permission.ts`加载后端接口路由配置。
> **提示**
> * 动态路由可以在系统管理-菜单管理进行新增和修改操作，前端加载会自动请求接口获取菜单信息并转换成前端对应的路由。
> * 动态路由在生产环境下会默认使用路由懒加载，实现方式参考loadView方法的判断。
### 常用方法
想要跳转到不同的页面，使用`router.push`方法
```Typescript
const router = useRouter();
router.push({ path: "/system/user" });
```
跳转页面并设置请求参数，使用`query`属性
```Typescript
const router = useRouter();
router.push({ path: "/system/user", query: {id: "1", name: "若依"} });
```
更多使用可以参考[vue-router](https://router.vuejs.org/zh/)官方文档。