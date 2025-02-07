# 组件使用
- - -

vue 注册组件的两种方式
在 `@/components` 下创建的.vue文件自动为全局组件，可直接在任意位置使用。

### 局部注册
在对应页使用`components`注册组件。
```typescript
<script setup lang=ts>
import ComponentA from './ComponentA.vue'
</script>

<template>
  <ComponentA />
</template>
```

### 全局注册
我们可以使用[ Vue 应用实例](https://cn.vuejs.org/guide/essentials/application.html)的 `.component()` 方法，让组件在当前 Vue 应用中全局可用。
```typescript
import { createApp } from 'vue'

const app = createApp({})

app.component(
  // 注册的名字
  'MyComponent',
  // 组件的实现
  {
    /* ... */
  }
)
```
如果使用单文件组件，你可以注册被导入的 `.vue` 文件：
```typescript
import MyComponent from './App.vue'

app.component('MyComponent', MyComponent)
```
`.component()` 方法可以被链式调用：
```typescript
app
  .component('ComponentA', ComponentA)
  .component('ComponentB', ComponentB)
  .component('ComponentC', ComponentC)
```
全局注册的组件可以在此应用的任意组件的模板中使用：
```Typescript
// 这在当前应用的任意组件中都可用
<ComponentA/>
<ComponentB/>
<ComponentC/>
```
所有的子组件也可以使用全局注册的组件，这意味着这三个组件也都可以在彼此内部使用。