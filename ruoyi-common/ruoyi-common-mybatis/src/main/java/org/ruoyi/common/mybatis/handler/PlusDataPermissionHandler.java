package org.ruoyi.common.mybatis.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.ruoyi.common.core.domain.dto.RoleDTO;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.annotation.DataColumn;
import org.ruoyi.common.mybatis.annotation.DataPermission;
import org.ruoyi.common.mybatis.enums.DataScopeType;
import org.ruoyi.common.mybatis.helper.DataPermissionHelper;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.*;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.function.Function;

/**
 * 数据权限过滤
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Slf4j
public class PlusDataPermissionHandler {

    /**
     * spel 解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * bean解析器 用于处理 spel 表达式中对 bean 的调用
     */
    private final BeanResolver beanResolver = new BeanFactoryResolver(SpringUtils.getBeanFactory());

    /**
     * 获取数据过滤条件的 SQL 片段
     *
     * @param where             原始的查询条件表达式
     * @param isSelect          是否为查询语句
     * @return 数据过滤条件的 SQL 片段
     */
    public Expression getSqlSegment(Expression where, boolean isSelect) {
        try {
            // 获取数据权限配置
            DataPermission dataPermission = getDataPermission();
            // 获取当前登录用户信息
            LoginUser currentUser = DataPermissionHelper.getVariable("user");
            if (ObjectUtil.isNull(currentUser)) {
                currentUser = LoginHelper.getLoginUser();
                DataPermissionHelper.setVariable("user", currentUser);
            }
            // 如果是超级管理员或租户管理员，则不过滤数据
            if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
                return where;
            }
            // 构造数据过滤条件的 SQL 片段
            String dataFilterSql = buildDataFilter(dataPermission, isSelect);
            if (StringUtils.isBlank(dataFilterSql)) {
                return where;
            }
            Expression expression = CCJSqlParserUtil.parseExpression(dataFilterSql);
            // 数据权限使用单独的括号 防止与其他条件冲突
            ParenthesedExpressionList<Expression> parenthesis = new ParenthesedExpressionList<>(expression);
            if (ObjectUtil.isNotNull(where)) {
                return new AndExpression(where, parenthesis);
            } else {
                return parenthesis;
            }
        } catch (JSQLParserException e) {
            throw new ServiceException("数据权限解析异常 => " + e.getMessage());
        } finally {
            DataPermissionHelper.removePermission();
        }
    }

    /**
     * 构建数据过滤条件的 SQL 语句
     *
     * @param dataPermission 数据权限注解
     * @param isSelect       标志当前操作是否为查询操作，查询操作和更新或删除操作在处理过滤条件时会有不同的处理方式
     * @return 构建的数据过滤条件的 SQL 语句
     * @throws ServiceException 如果角色的数据范围异常或者 key 与 value 的长度不匹配，则抛出 ServiceException 异常
     */
    private String buildDataFilter(DataPermission dataPermission, boolean isSelect) {
        // 更新或删除需满足所有条件
        String joinStr = isSelect ? " OR " : " AND ";
        if (StringUtils.isNotBlank(dataPermission.joinStr())) {
            joinStr = " " + dataPermission.joinStr() + " ";
        }
        LoginUser user = DataPermissionHelper.getVariable("user");
        Object defaultValue = "-1";
        NullSafeStandardEvaluationContext context = new NullSafeStandardEvaluationContext(defaultValue);
        context.addPropertyAccessor(new NullSafePropertyAccessor(context.getPropertyAccessors().get(0), defaultValue));
        context.setBeanResolver(beanResolver);
        DataPermissionHelper.getContext().forEach(context::setVariable);
        Set<String> conditions = new HashSet<>();
        // 优先设置变量
        List<String> keys = new ArrayList<>();
        Map<DataColumn, Boolean> ignoreMap = new HashMap<>();
        for (DataColumn dataColumn : dataPermission.value()) {
            if (dataColumn.key().length != dataColumn.value().length) {
                throw new ServiceException("角色数据范围异常 => key与value长度不匹配");
            }
            // 包含权限标识符 这直接跳过
            if (StringUtils.isNotBlank(dataColumn.permission()) &&
                CollUtil.contains(user.getMenuPermission(), dataColumn.permission())
            ) {
                ignoreMap.put(dataColumn, Boolean.TRUE);
                continue;
            }
            // 设置注解变量 key 为表达式变量 value 为变量值
            for (int i = 0; i < dataColumn.key().length; i++) {
                context.setVariable(dataColumn.key()[i], dataColumn.value()[i]);
            }
            keys.addAll(Arrays.stream(dataColumn.key()).map(key -> "#" + key).toList());
        }

        for (RoleDTO role : user.getRoles()) {
            user.setRoleId(role.getRoleId());
            // 获取角色权限泛型
            DataScopeType type = DataScopeType.findCode(role.getDataScope());
            if (ObjectUtil.isNull(type)) {
                throw new ServiceException("角色数据范围异常 => " + role.getDataScope());
            }
            // 全部数据权限直接返回
            if (type == DataScopeType.ALL) {
                return StringUtils.EMPTY;
            }
            boolean isSuccess = false;
            for (DataColumn dataColumn : dataPermission.value()) {
                // 包含权限标识符 这直接跳过
                if (ignoreMap.containsKey(dataColumn)) {
                    conditions.add(joinStr + " 1 = 1 ");
                    isSuccess = true;
                    continue;
                }
                // 不包含 key 变量 则不处理
                if (!StringUtils.containsAny(type.getSqlTemplate(), keys.toArray(String[]::new))) {
                    continue;
                }
                // 当前注解不满足模板 不处理
                if (!StringUtils.containsAny(type.getSqlTemplate(), dataColumn.key())) {
                    continue;
                }
                // 忽略数据权限 防止spel表达式内有其他sql查询导致死循环调用
                String sql = DataPermissionHelper.ignore(() ->
                    parser.parseExpression(type.getSqlTemplate(), parserContext).getValue(context, String.class)
                );
                // 解析sql模板并填充
                conditions.add(joinStr + sql);
                isSuccess = true;
            }
            // 未处理成功则填充兜底方案
            if (!isSuccess && StringUtils.isNotBlank(type.getElseSql())) {
                conditions.add(joinStr + type.getElseSql());
            }
        }

        if (CollUtil.isNotEmpty(conditions)) {
            String sql = StreamUtils.join(conditions, Function.identity(), "");
            return sql.substring(joinStr.length());
        }
        return StringUtils.EMPTY;
    }

    /**
     * 根据映射语句 ID 或类名获取对应的 DataPermission 注解对象
     *
     * @return DataPermission 注解对象，如果不存在则返回 null
     */
    public DataPermission getDataPermission() {
        return DataPermissionHelper.getPermission();
    }

    /**
     * 检查给定的映射语句 ID 是否有效，即是否能够找到对应的 DataPermission 注解对象
     *
     * @return 如果找到对应的 DataPermission 注解对象，则返回 false；否则返回 true
     */
    public boolean invalid() {
        return getDataPermission() == null;
    }

    /**
     * 对所有null变量找不到的变量返回默认值
     */
    @AllArgsConstructor
    private static class NullSafeStandardEvaluationContext extends StandardEvaluationContext {

        private final Object defaultValue;

        @Override
        public Object lookupVariable(String name) {
            Object obj = super.lookupVariable(name);
            // 如果读取到的值是 null，则返回默认值
            if (obj == null) {
                return defaultValue;
            }
            return obj;
        }

    }

    /**
     * 对所有null变量找不到的变量返回默认值 委托模式 将不需要处理的方法委托给原处理器
     */
    @AllArgsConstructor
    private static class NullSafePropertyAccessor implements PropertyAccessor {

        private final PropertyAccessor delegate;
        private final Object defaultValue;

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return delegate.getSpecificTargetClasses();
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            return delegate.canRead(context, target, name);
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            TypedValue value = delegate.read(context, target, name);
            // 如果读取到的值是 null，则返回默认值
            if (value.getValue() == null) {
                return new TypedValue(defaultValue);
            }
            return value;
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
            return delegate.canWrite(context, target, name);
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
            delegate.write(context, target, name, newValue);
        }
    }

}
