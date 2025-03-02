package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * PPT模板筛选查询参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptTemplateFilterDto {
    // 模板类型（必传）：1系统模板、4用户自定义模板
    private Integer type;

    // 类目筛选 ['年终总结', '教育培训', '医学医疗', '商业计划书', '企业介绍', '毕业答辩', '营销推广', '晚会表彰', '个人简历']
    private String category;

    // 风格筛选 ['扁平简约', '商务科技', '文艺清新', '卡通手绘', '中国风', '创意时尚', '创意趣味']
    private String style;

    // 主题颜色筛选 ['#FA920A', '#589AFD', '#7664FA', '#65E5EC', '#61D328', '#F5FD59', '#E05757', '#8F5A0B', '#FFFFFF', '#000000']
    private String themeColor;
}
