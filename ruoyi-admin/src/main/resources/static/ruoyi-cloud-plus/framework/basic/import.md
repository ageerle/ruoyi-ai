# 导入功能
- - -

在本框架中引入了 `Easy Excel` 依赖（对 `Apache POI`进行了封装以及扩展），可以对数据进行导入操作（即读 Excel）。

## 导入功能使用流程说明

### 步骤一：定义导入实体对象

以框架中 `SysUserImportVo` 为例：

```java
    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户序号")
    private Long userId;

    // .......................
    
    /**
     * 用户性别
     */
    @ExcelProperty(value = "用户性别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_user_sex")
    private String sex;

    /**
     * 帐号状态（0正常 1停用）
     */
    @ExcelProperty(value = "帐号状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;
```

> 说明：<br>
> 1. 使用 `@ExcelProperty` 注解标注需要导入的属性。
> 2. 注解 `@ExcelProperty` 中 `value` 属性代表表格头部标题字段，`converter` 代表使用的转换器，后面会详细说明。
> 3. 注解 `@ExcelDictFormat` 为自定义注解，与自定义转换器结合使用，同样在后面进行详细说明。
> 4. 对象禁止使用链式注解 `@Accessors(chain = true)`，会找不到set方法。

### 步骤二：使用导入方法

以框架中 `SysUserController#importData` 方法为例：

```Java
    /**
     * 导入数据
     *
     * @param file          导入文件
     * @param updateSupport 是否更新已存在数据
     */
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @SaCheckPermission("system:user:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        // 导入方法
        ExcelResult<SysUserImportVo> result = ExcelUtil.importExcel(file.getInputStream(), SysUserImportVo.class, new SysUserImportListener(updateSupport));
        return R.ok(result.getAnalysis());
    }
```
> 说明：<br>
> 使用 `ExcelUtil.importExcel` 方法完成导出功能，上述 Demo 传入参数分别是：导入文件流，导入对象类型，导入监听器 `SysUserImportListener`。

## 框架工具使用说明

### 1：字典转换器

字典转换器 `ExcelDictConvert` 与自定义注解 `@ExcelDictFormat` 结合使用，标注在需要转换的属性上。

使用方式一：

```Java
    /**
     * 用户性别
     */
    @ExcelProperty(value = "用户性别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_user_sex")
    private String sex;
```

使用方式二：

```Java
    /**
     * 用户性别
     */
    @ExcelProperty(value = "用户性别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp="0=男,1=女,2=未知", separator=",")
    private String sex;
```

`@ExcelDictFormat` 注解属性说明：

| 属性名称             | 属性类型   | 默认值 | 说明                                |
|------------------|--------|-----|-----------------------------------|
| dictType         | String | ""  | 字典的type值 (如: sys_user_sex)        |
| readConverterExp | String | ""  | 读取内容转表达式 (如: 0=男,1=女,2=未知)        |
| separator        | String | "," | 与 readConverterExp 属性结合使用，表达式的分隔符 |

### 2：枚举转换器

字典转换器 `ExcelEnumConvert` 与自定义注解 `@ExcelEnumFormat` 结合使用，标注在需要转换的属性上。

使用方式：

```Java
    /**
     * 用户类型
     * </p>
     * 使用ExcelEnumFormat注解需要进行下拉选的部分
     */
    @ExcelProperty(value = "用户类型", index = 1, converter = ExcelEnumConvert.class)
    @ExcelEnumFormat(enumClass = UserStatus.class, textField = "info")
    private String userStatus;
```

`@ExcelEnumFormat` 注解属性说明：

| 属性名称      | 属性类型       | 默认值  | 说明                           |
|-----------|------------|------|------------------------------|
| enumClass | Enum Class | -    | 字典枚举类型                       |
| codeField | String     | code | 字典枚举类中对应的 code 属性名称，默认为 code |
| textField | String     | text | 字典枚举类中对应的 text 属性名称，默认为 text |


### 3：导入监听器

#### 3.1：ExcelListener 监听器接口

`ExcelListener` 扩展了 `ReadListener` 接口，增加了获取结果方法。

![输入图片说明](https://foruda.gitee.com/images/1700181723794469524/99bf83c9_4959041.png "屏幕截图")

#### 3.2：DefaultExcelListener 默认监听器

`DefaultExcelListener` 默认监听器在读 Excel 时调用，主要对数据进行校验、解析、异常处理、返回结果等。导入操作时如果没有特别指定则使用该监听器。

#### 3.3：SysUserImportListener 用户导入监听器

`SysUserImportListener` 用户导入监听器是在用户导入时调用的监听器。

该监听器重写了 `invoke` 反射接口，对导入的用户数据进行了校验；重写了 `getExcelResult` 获取结果接口，返回结果数据。 

#### 3.4：ExportDemoListener 带下拉框的导入监听器

`ExportDemoListener` 是对带有下拉框的 Excel 进行处理的导入监听器。

## Easy Excel 常用注解

`Easy Excel` 提供了丰富的注解可以对导出对象进行定制化操作，这里的注解说明针对的是原生注解。

| 类型    | 注解名称                    | 使用举例                                                                                                       | 说明                                                                                                       |
|-------|-------------------------|------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| 格式化注解 | @DateTimeFormat         | @DateTimeFormat(value=格式化值)                                                                                | 对字符串进行日期格式化 (参照 `java.text.SimpleDateFormat` 书写即可)                                                       |
| 格式化注解 | @NumberFormat           | @NumberFormat(value=格式化值, roundingMode=舍入模式)                                                               | 对字符串进行数值格式化 (参照 `java.text.DecimalFormat` 书写即可, `roundingMode` 默认 `RoundingMode.HALF_UP`)                |
| 属性注解  | @ExcelIgnore            | @ExcelIgnore                                                                                               | 导出忽略该字段                                                                                                  |
| 属性注解  | @ExcelIgnoreUnannotated | @ExcelIgnoreUnannotated                                                                                    | 默认不管加不加 `@ExcelProperty` 的注解的所有字段都会参与读写，加了 `@ExcelIgnoreUnannotated` 注解以后，不加 `@ExcelProperty` 注解的字段就不会参与 |
| 属性注解  | @ExcelProperty          | @ExcelProperty(value=值, order=排序值, index=下标, converter=转换器)                                                | 默认按照对象属性顺序导出，如果设置了 `order` 以及 `index`，优先级 `index` > `order` > 默认；converter 可以自定义                         |

## 扩展使用

### 扩展一：自定义转换器实现

由于业务需要，原生注解不一定能够符合需要，因而衍生出了自定义转换器。能够实现定制化的内容转换需要。
以下以框架中的字典转换器 `ExcelDictConvert` 为例进行说明。

字典转换器 `ExcelDictConvert`，字典转换器使用了自定义注解 `@ExcelDictFormat` 配合使用。

_**注：自定义转换器并非一定需要自定义注解，也可以针对已有的注解进行自定义转换实现。**_

#### 实现方式

自定义转换器需要实现 `com.alibaba.excel.converters.Converter` 接口，实现接口中的方法。

![输入图片说明](https://foruda.gitee.com/images/1700104014304819918/33eb0c42_4959041.png "屏幕截图")

转换方法 `ExcelDictConvert#convertToJavaData` ：

![输入图片说明](https://foruda.gitee.com/images/1700182975516396213/d3c020f9_4959041.png "屏幕截图")

### 扩展二：自定义监听器实现

自定义监听器主要用于在读取解析 Excel 数据时进行自定义操作。
以下以框架中的用户导入监听器 `SysUserImportListener` 为例进行说明。

#### 实现方式
1. 继承分析事件监听器 `AnalysisEventListener` 以及实现 Excel 监听器 `ExcelListener`。 

![输入图片说明](https://foruda.gitee.com/images/1700184652693497753/09333dac_4959041.png "屏幕截图")

2. 显示使用构造函数，否则将导致空指针。

![输入图片说明](https://foruda.gitee.com/images/1700184759075616584/cf05b0ed_4959041.png "屏幕截图")

3. 实现 `invoke` 方法，对数据进行解析操作，可以在此方法对数据进行合法性判断。

4. 实现 `getExcelResult` 方法，对结果进行操作，例如返回成功、失败的统计数据。

## 更多功能

更多导入功能使用可以参照 `Easy Excel` [官方文档](https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read)。