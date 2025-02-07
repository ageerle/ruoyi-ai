# 导出功能

- - -

在本框架中引入了 `Easy Excel` 依赖（对 `Apache POI`进行了封装以及扩展），可以对数据进行导出操作（即写 Excel）。

[EasyExcel 文档地址](https://easyexcel.opensource.alibaba.com/)

## 导出功能使用流程说明

### 步骤一：定义导出实体对象

以框架中 `SysUserExportVo` 为例：

```Java
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
> 1. 使用 `@ExcelProperty` 注解标注需要导出的属性。
> 2. 注解 `@ExcelProperty` 中 `value` 属性代表表格头部标题字段，`converter` 代表使用的转换器，后面会详细说明。
> 3. 注解 `@ExcelDictFormat` 为自定义注解，与自定义转换器结合使用，同样在后面进行详细说明。

### 步骤二：使用导出方法

以框架中 `SysUserController#export` 方法为例：

```Java
    /**
     * 导出用户列表
     */
    @PostMapping("/export")
    public void export(SysUserBo user, HttpServletResponse response) {
        // 根据参数查询导出的用户列表数据
        List<SysUserVo> list = userService.selectUserList(user);
        // 将列表转换为导出对象列表
        List<SysUserExportVo> listVo = MapstructUtils.convert(list, SysUserExportVo.class);
        // 导出方法
        ExcelUtil.exportExcel(listVo, "用户数据", SysUserExportVo.class, response);
    }
```

> 说明：<br>
> 使用 `ExcelUtil.exportExcel` 方法完成导出功能，上述 Demo 传入参数分别是：导出对象集合，Excel sheet 表名称，导出对象类型，response。

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

### 3：合并单元格

`@CellMerge` 注解用于合并相同的列数据，需要结合 `CellMergeStrategy` 策略使用，标注在需要转换的属性上。

使用方式：

步骤一：在属性标注 `@CellMerge` 注解：
```Java
    /**
     * 部门id
     */
    @CellMerge
    @ExcelProperty(value = "部门id")
    private Long deptId;
```

`@CellMerge` 注解属性说明：

| 属性名称    | 属性类型     | 默认值 | 说明                           |
|---------|----------|-----|------------------------------|
| index   | int      | -1  | 合并列的下标，建议使用默认值               |
| mergeBy | String[] | {}  | 合并需要依赖的其他字段名称(基于这个字段内容做合并条件) |

步骤二：导出方法开启合并：
```Java
    /**
     * 导出测试单表列表
     */
    @PostMapping("/export")
    public void export(@Validated TestDemoBo bo, HttpServletResponse response) {
        List<TestDemoVo> list = testDemoService.queryList(bo);
        // 参数 true 表示开启合并单元格策略
        ExcelUtil.exportExcel(list, "测试单表", TestDemoVo.class, true, response);
    } 
```
![输入图片说明](https://foruda.gitee.com/images/1700128921644543994/e8d4704f_1766278.png "屏幕截图")

### 4：复杂 Excel 导出示例
`TestExcelController` 提供了几种导出示例，如果需要可以参照相应方法进行导出。

#### 4.1：单列表多数据导出（模板导出）

模板内容：

![输入图片说明](https://foruda.gitee.com/images/1700124852002972562/d9f57a8c_4959041.png "屏幕截图")

模板位置：`ruoyi-modules/ruoyi-demo/src/main/resources/excel/`

导出示例代码：参考 demo 模块 `TestExcelController` 模板写法请查看 `EasyExcel` 文档

导出结果：

![输入图片说明](https://foruda.gitee.com/images/1700124885532359879/0d011d05_4959041.png "屏幕截图")

#### 4.2：多列表多数据导出（模板导出）

模板内容：

![输入图片说明](https://foruda.gitee.com/images/1700125025931981176/105dbaaa_4959041.png "屏幕截图")

模板位置：`ruoyi-modules/ruoyi-demo/src/main/resources/excel/`

导出示例代码：参考 demo 模块 `TestExcelController` 模板写法请查看 `EasyExcel` 文档

导出结果：

![输入图片说明](https://foruda.gitee.com/images/1700125054011300002/71869c1d_4959041.png "屏幕截图")

#### 4.3：导出下拉框

`ExcelDictFormat` 注解指定的字典项默认都会转换成下拉框

自定义导出省市区下拉框示例代码：参考 demo 模块 `TestExcelController`

导出结果：

![输入图片说明](https://foruda.gitee.com/images/1700125265411678973/7f767719_4959041.png "屏幕截图")

## Easy Excel 常用注解

`Easy Excel` 提供了丰富的注解可以对导出对象进行定制化操作，这里的注解说明针对的是原生注解，自定义注解会结合转换器一起进行说明。

| 类型    | 注解名称                    | 使用举例                                                                                                       | 说明                                                                                                       |
|-------|-------------------------|------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| 格式化注解 | @DateTimeFormat         | @DateTimeFormat(value=格式化值)                                                                                | 对字符串进行日期格式化 (参照 `java.text.SimpleDateFormat` 书写即可)                                                       |
| 格式化注解 | @NumberFormat           | @NumberFormat(value=格式化值, roundingMode=舍入模式)                                                               | 对字符串进行数值格式化 (参照 `java.text.DecimalFormat` 书写即可, `roundingMode` 默认 `RoundingMode.HALF_UP`)                |
| 样式注解  | @ColumnWidth            | @ColumnWidth(value=值)                                                                                      | 设置列宽                                                                                                     |
| 样式注解  | @ContentFontStyle       | @ContentFontStyle(color=颜色)                                                                                | 可以设置字体类型，颜色，粗细，是否斜体，下划线等，具体可查看注解 `@ContentFontStyle`                                                     |
| 样式注解  | @ContentLoopMerge       | @ContentLoopMerge(eachRow=行值, columnExtend=列值)                                                             | 设置循环合并的区域                                                                                                |
| 样式注解  | @ContentRowHeight       | @ContentRowHeight(value=值)                                                                                 | 设置内容行高                                                                                                   |
| 样式注解  | @ContentStyle           | -                                                                                                          | 设置单元格样式，具体可查看注解 `@ContentStyle`                                                                          |
| 样式注解  | @HeadFontStyle          | @HeadFontStyle(color=颜色)                                                                                   | 设置表头字体格式，类似 `@ContentFontStyle`，具体可查看注解 `@HeadFontStyle`                                                 |
| 样式注解  | @HeadRowHeight          | @HeadRowHeight(value=值)                                                                                    | 设置表头行高                                                                                                   |
| 样式注解  | @HeadStyle              | -                                                                                                          | 设置表头样式，具体可查看注解 `@HeadStyle`                                                                              |
| 样式注解  | @OnceAbsoluteMerge      | @OnceAbsoluteMerge(firstRowIndex=开始行下标, lastRowIndex=结束行下标, firstColumnIndex=开始列下标, lastColumnIndex=结束列下标) | 根据设置值合并单元格                                                                                               |
| 属性注解  | @ExcelIgnore            | @ExcelIgnore                                                                                               | 导出忽略该字段                                                                                                  |
| 属性注解  | @ExcelIgnoreUnannotated | @ExcelIgnoreUnannotated                                                                                    | 默认不管加不加 `@ExcelProperty` 的注解的所有字段都会参与读写，加了 `@ExcelIgnoreUnannotated` 注解以后，不加 `@ExcelProperty` 注解的字段就不会参与 |
| 属性注解  | @ExcelProperty          | @ExcelProperty(value=值, order=排序值, index=下标, converter=转换器)                                                | 默认按照对象属性顺序导出，如果设置了 `order` 以及 `index`，优先级 `index` > `order` > 默认；converter 可以自定义                         |

## 扩展说明

### 自定义转换器实现

由于业务需要，原生注解不一定能够符合需要，因而衍生出了自定义转换器。能够实现定制化的内容转换需要。
以下以框架中的字典转换器 `ExcelDictConvert` 为例进行说明。

字典转换器 `ExcelDictConvert`，字典转换器使用了自定义注解 `@ExcelDictFormat` 配合使用。

_**注：自定义转换器并非一定需要自定义注解，也可以针对已有的注解进行自定义转换实现。**_

#### 实现方式

自定义转换器需要实现 `com.alibaba.excel.converters.Converter` 接口，实现接口中的方法。

![输入图片说明](https://foruda.gitee.com/images/1700104014304819918/33eb0c42_4959041.png "屏幕截图")

转换方法 `ExcelDictConvert#convertToExcelData` ：

![输入图片说明](https://foruda.gitee.com/images/1700104426131801297/72931ef0_4959041.png "屏幕截图")

## 更多功能

更多导出功能使用可以参照 `Easy Excel` [官方文档](https://easyexcel.opensource.alibaba.com/docs/current/api/write)。