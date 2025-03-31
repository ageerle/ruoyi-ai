### 序列化

#### 序列化基本数据类型

1. escape(key)
2. escape(value)
3. key@=value/

#### 序列化Map

1. escape(key)
2. value for each
    1. escape(key)
    2. escape(value)
    3. escape(key@=value/)
3. escape(value)
4. key@=value/

### 反序列化

#### 反序列化基本数据类型

1. spilt("/"):
   type@=chatmsg

2. split("@="):
    1. key: unescape(key) = el
    2. unescape(value).endsWith("/") = false
       2.1 value = chatmsg

#### 反序列化Map

1. spilt("/"):
   el@=eid@AA=1@ASetp@AA=1@ASsc@AA=1@AS

2. split("@="):
    1. key: unescape = el
    2. unescape(value).endsWith("/") = true
       eid@A=1/etp@A=1/sc@A=1/
        1. split("/")
           eid@A=1
           etp@A=1
           sc@A=1
        2. unescape:
           eid@=1
           etp@=1
           sc@=1
        3. for each
            1. split("@=")
            2. unescape(key)
            3. unescape(value)
        4. value: map