# 请求流程
- - -

### 交互流程
一个完整的前端UI交互到服务器端处理流程是这样的：  

1. UI 组件交互操作；
2. 调用统一管理的 api service 请求函数；
3. 使用封装的 request.js 发送请求；
4. 获取服务端返回；
5. 更新 data；

为了方便管理维护，统一的请求处理都放在`@/src/api`文件夹中，并且一般按照`model`维度进行拆分文件，如：
```
api/
  system/
    user/
      index.ts
      types.ts
    role/
      index.ts
      types.ts
  monitor/
    operlog/
      index.ts
      types.ts
    logininfor/
      index.ts
      types.ts
  ...
```
> **提示**  
> 其中`@/src/utils/request.ts`是基于 axios 的封装，便于统一处理 POST，GET 等请求参数，请求头，以及错误提示信息等。 它封装了全局request拦截器、response拦截器、统一的错误处理、统一做了超时处理、baseURL设置等。

### 请求示例
```typescript
// @/api/system/user/index.ts
import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { UserQuery, UserVO } from './types';

export const listUser = (query: UserQuery): AxiosPromise<UserVO[]> => {
  return request({
    url: '/system/user/list',
    method: 'get',
    params: query
  });
};

// @/views/system/user/index.vue
import api from '@/api/system/user';
const res = await api.listUser(proxy?.addDateRange(queryParams.value, dateRange.value));
```
> **提示**  
> 如果有不同的`baseURL`，直接通过覆盖的方式，让它具有不同的`baseURL`。
> ```typescript
> export const listUser = (query: UserQuery): AxiosPromise<UserVO[]> => {
>   return request({
>     url: '/system/user/list',
>     method: 'get',
>     params: query,
>     baseURL: process.env.BASE_API
>   });
> };
> ```
