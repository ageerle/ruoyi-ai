package com.xmzs.common.chat.constant;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @since  2023-03-06
 */
public class OpenAIConst {

    public final static String OPENAI_HOST = "https://api.openai.com/";

    public final static int SUCCEED_CODE = 200;

    /** GPT3扣除费用 */
    public final static double GPT3_COST = 0.05;

    /** GPT4扣除费用 */
    public final static double GPT4_COST = 0.2;

    /** DALL普通绘图扣除费用 */
    public final static double DALL3_COST = 0.3;

    /** DALL高清绘图扣除费用 */
    public final static double DALL3_HD_COST = 0.5;

    /** MJ操作类型1(变化、变焦、文生图、图生图、局部重绘、混图)扣除费用 */
    public final static double MJ_COST_TYPE1 = 0.3;

    /** MJ操作类型2(换脸、放大、图生文、prompt分析)扣除费用 */
    public final static double MJ_COST_TYPE2 = 0.1;

    /** MJ操作类型3(查询任务进度、获取seed)扣除费用 */
    public final static double MJ_COST_TYPE3 = 0.0;

    /** 默认账户余额 */
    public final static double USER_BALANCE = 5;
}
