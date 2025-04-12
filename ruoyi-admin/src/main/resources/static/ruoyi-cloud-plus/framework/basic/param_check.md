# 参数校验
- - -

参数校验在日常开发中十分常见，在本框架中引入了 `spring-boot-starter-validation` 依赖，底层基于 `hibernate-validator`，可以对参数进行校验。

## 参数校验使用

### 方法一：使用 `@Validated` 注解

#### 步骤一：标注 `@Validated`

`@Validated` 可以标注在类上，或者是参数前。

```Java
/** 标注在类上 **/
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody LoginBody body) {
        // ...
    }

}
```

```Java
/** 标注在参数前 **/
@PostMapping
public R<Void> add(@Validated @RequestBody SysUserBo user) {
    // ...
}
```

#### 步骤二：标注校验注解

在参数中加入校验注解。

```Java
public class SysUserBo {

    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过{max}个字符")
    private String userName;
    
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;
    
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;
    
}
```

常见校验注解见文末附表。

_注：message 支持 EL 表达式，{max} 直接读取前面的参数值。_

### 方法二：使用校验工具类 `ValidatorUtils`

`org.dromara.common.core.utils.ValidatorUtils`

![输入图片说明](https://foruda.gitee.com/images/1700050047426137432/206bd032_4959041.png "屏幕截图")

使用方式 1：校验所有带有校验注解的属性

```Java
// 校验所有带有校验注解的属性
ValidatorUtils.validate(object);
```

使用方式 2：按照分组校验属性（可以传多个分组）

```Java
// 按照分组校验属性（可以传多个分组）
ValidatorUtils.validate(object, group);
```

## 扩展使用

### 扩展一：自定义校验注解

除了已有的校验注解以外，可以结合业务进行自定义。

以框架中的 `@Xss` 注解为例进行说明。

```Java
@Xss(message = "用户账号不能包含脚本字符")
@NotBlank(message = "用户账号不能为空")
@Size(min = 0, max = 30, message = "用户账号长度不能超过{max}个字符")
private String userName;
```

#### 1：新增 `@Xss` 注解

`org.dromara.common.core.xss.Xss`

![输入图片说明](https://foruda.gitee.com/images/1700048074014527096/b4e230c2_4959041.png "屏幕截图")

#### 2：自定义校验器

自定义校验器实现 `jakarta.validation.ConstraintValidator` 接口。

`org.dromara.common.core.xss.XssValidator`

![输入图片说明](https://foruda.gitee.com/images/1700048474563719650/f9172bdc_4959041.png "屏幕截图")

### 扩展二：自定义分组校验

同一个对象在不同的请求中需要校验的参数不同，则可以使用分组校验。

#### 1：自定义分组

![输入图片说明](https://foruda.gitee.com/images/1700049439236073123/9e0d2e16_4959041.png "屏幕截图")

#### 2：`@Validated` 注解指定分组

![输入图片说明](https://foruda.gitee.com/images/1700049302803077030/c2a985aa_4959041.png "屏幕截图")

#### 3：校验注解中指定分组

![输入图片说明](https://foruda.gitee.com/images/1700049205699437759/96babbd6_4959041.png "屏幕截图")

## 附录：常用校验注解

| 注解               | 使用（只列举特殊参数值）                         | 参数类型                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 说明                                    |
|------------------|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| @AssertFalse     | @AssertFalse                         | boolean / Boolean                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | 元素值必须为 false                          |
| @AssertTrue      | @AssertTrue                          | boolean / Boolean                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | 元素值必须为 true                           |
| @DecimalMax      | @DecimalMax(value=值)                 | - BigDecimal <br> - BigInteger  <br> - CharSequence <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                            | 元素必须是一个数字，其值必须小于或等于指定的最大值             |
| @DecimalMin      | @DecimalMin(value=值)                 | - BigDecimal <br> - BigInteger  <br> - CharSequence <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                            | 元素必须是一个数字，其值必须大于或等于指定的最小值             |
| @Digits          | @Digits(integer=整数位值, fraction=小数位值) | - BigDecimal <br> - BigInteger  <br> - CharSequence <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                            | 元素必须符合整数位以及小数位范围值                     |
| @Email           | @Email(regexp=正则表达式, flags=标志)       | CharSequence                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | 元素是否符合正则表达式（正则表达式非必传）                 |
| @Future          | @Future                              | - java.util.Date <br> - java.util.Calendar <br> - java.time.Instant <br> - java.time.LocalDate <br> - java.time.LocalDateTime <br> - java.time.LocalTime <br> - java.time.MonthDay <br> - java.time.OffsetDateTime <br> - java.time.OffsetTime <br> - java.time.Year <br> - java.time.YearMonth <br> - java.time.ZonedDateTime <br> - java.time.chrono.HijrahDate <br> - java.time.chrono.JapaneseDate <br> - java.time.chrono.MinguoDate <br> - java.time.chrono.ThaiBuddhistDate | 元素必须是未来的时刻、日期或时间                      |
| @FutureOrPresent | @FutureOrPresent                     | 同 @Future                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 元素必须是当前或未来的时刻、日期或时间                   |
| @Length          | @Length(min=最小值, max=最大值)            | - CharSequence                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 验证字符串是否在包含的 min 和 max 之间              |
| @Max             | @Max(value=值)                        | - BigDecimal <br> - BigInteger  <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                                | 元素必须是一个数字，其值必须小于或等于指定的最大值             |
| @Min             | @Min(value=值)                        | - BigDecimal <br> - BigInteger  <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                                | 元素必须是一个数字，其值必须大于或等于指定的最小值             |
| @Negative        | @Negative                            | - BigDecimal <br> - BigInteger  <br> - byte，short，int，long，float，double 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                      | 元素必须是一个严格的负数（即 0 被视为无效值）              |
| @NegativeOrZero  | @NegativeOrZero                      | - BigDecimal <br> - BigInteger  <br> - byte，short，int，long，float，double 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                      | 元素必须为负数或 0                            |
| @NotBlank        | @NotBlank                            | CharSequence                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | 元素不能为 null，并且必须至少包含一个非空白字符            |
| @NotEmpty        | @NotEmpty                            | - CharSequence <br> - Collection <br> - Map <br> - Array                                                                                                                                                                                                                                                                                                                                                                                                                           | 元素不能为 null 或空集合                       |
| @NotNull         | @NotNull                             | 不限类型                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 元素不能为 null                            |
| @Null            | @Null                                | 不限类型                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 元素必须为 null                            |
| @Past            | @Past                                | 同 @Future                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 元素必须是过去的瞬间、日期或时间                      |
| @PastOrPresent   | @PastOrPresent                       | 同 @Future                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 元素必须是过去或现在的瞬间、日期或时间                   |
| @Pattern         | @Pattern(regexp=正则表达式, flags=标志)     | CharSequence                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | 元素必须与指定的正则表达式匹配（正则表达式遵循 Java 正则表达式约定） |
| @Positive        | @Positive                            | - BigDecimal <br> - BigInteger  <br> - byte，short，int，long，float，double 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                      | 元素必须是一个严格的正数（即 0 被视为无效值）              |
| @PositiveOrZero  | @PositiveOrZero                      | - BigDecimal <br> - BigInteger  <br> - byte，short，int，long，float，double 及其包装类                                                                                                                                                                                                                                                                                                                                                                                                      | 元素必须为正数或 0                            |
| @Range           | @Range(min=最小值, max=最大值)             | - BigDecimal <br> - BigInteger  <br> - CharSequence <br> - byte, short, int, long 及其包装类                                                                                                                                                                                                                                                                                                                                                                                            | 验证元素是否在包含的 min 和 max 之间               |
| @Size            | @Size(min=最小值, max=最大值)              | - CharSequence <br> - Collection <br> - Map <br> - Array                                                                                                                                                                                                                                                                                                                                                                                                                           | 验证元素是否在包含的 min 和 max 之间               |
| @Valid           | @Valid                               | 对象                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | 级联验证                                  |

更多注解可参考包： `org.hibernate.validator`