package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.PaymentOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 支付订单视图对象 payment_orders
 *
 * @author Lion Li
 * @date 2024-04-16
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PaymentOrder.class)
public class PaymentOrdersVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 订单编号
     */
    @ExcelProperty(value = "订单编号")
    private String orderNo;

    /**
     * 订单名称
     */
    @ExcelProperty(value = "订单名称")
    private String orderName;

    /**
     * 金额
     */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /**
     * 支付状态
     */
    @ExcelProperty(value = "支付状态")
    private String paymentStatus;

    /**
     * 支付方式
     */
    @ExcelProperty(value = "支付方式")
    private String paymentMethod;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户名称")
    private String userName;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 二维码网络地址
     */
    private String url;

    /**
     * 创建时间
     */
    private Date createTime;

}
