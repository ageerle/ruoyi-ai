# 事务相关
- - -
若依文档对事务注解的描述 [关于事务](https://doc.ruoyi.vip/ruoyi/document/htsc.html#%E4%BA%8B%E5%8A%A1%E7%AE%A1%E7%90%86)  以下对多数据源事务做补充:

## 多后端多数据源事务

框架支持对接 `seata` 保证分布式多数据源事务<br>
详情参考多数据源框架文档连接: https://www.kancloud.cn/tracy5546/dynamic-datasource/2268607

## 本地多数据源事务
请使用 `@DSTransactional` 注解 会代理 `@DS` 注解切换后的数据源事务做回滚处理<br>
只要 `@DSTransactional` 注解下任一环节发生异常，则全局多数据源事务回滚。<br>
如果BC上也有 `@DSTransactional` 会有影响吗？答：没有影响的。

```java
//如AService调用BService和CService的方法，A,B,C分别对应不同数据源。

public class AService {
    
    @DS("a")//如果a是默认数据源则不需要DS注解。
    @DSTransactional
    public void dosomething(){
        BService.dosomething();
        CService.dosomething();
    }
}

public class BService {
    
    @DS("b")
    public void dosomething(){
        //dosomething
    }
}

public class CService {
    
    @DS("c")
    public void dosomething(){
        //dosomething
    }
}
```


