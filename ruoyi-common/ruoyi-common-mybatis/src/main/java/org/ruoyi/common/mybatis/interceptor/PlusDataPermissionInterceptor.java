package org.ruoyi.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.ruoyi.common.mybatis.handler.PlusDataPermissionHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据权限拦截器
 *
 * @author Lion Li
 * @version 3.5.0
 */
public class PlusDataPermissionInterceptor extends JsqlParserSupport implements InnerInterceptor {

    private final PlusDataPermissionHandler dataPermissionHandler = new PlusDataPermissionHandler();

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 检查忽略注解
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            return;
        }
        // 检查是否无效 无数据权限注解
        if (dataPermissionHandler.isInvalid(ms.getId())) {
            return;
        }
        // 解析 sql 分配对应方法
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(parserSingle(mpBs.sql(), ms.getId()));
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
                return;
            }
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            mpBs.sql(parserMulti(mpBs.sql(), ms.getId()));
        }
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect plainSelect) {
            this.setWhere(plainSelect, (String) obj);
        } else if (selectBody instanceof SetOperationList setOperationList) {
            List<SelectBody> selectBodyList = setOperationList.getSelects();
            selectBodyList.forEach(s -> this.setWhere((PlainSelect) s, (String) obj));
        }
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        Expression sqlSegment = dataPermissionHandler.getSqlSegment(update.getWhere(), (String) obj, false);
        if (null != sqlSegment) {
            update.setWhere(sqlSegment);
        }
    }

    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        Expression sqlSegment = dataPermissionHandler.getSqlSegment(delete.getWhere(), (String) obj, false);
        if (null != sqlSegment) {
            delete.setWhere(sqlSegment);
        }
    }

    /**
     * 设置 where 条件
     *
     * @param plainSelect       查询对象
     * @param mappedStatementId 执行方法id
     */
    protected void setWhere(PlainSelect plainSelect, String mappedStatementId) {
        Expression sqlSegment = dataPermissionHandler.getSqlSegment(plainSelect.getWhere(), mappedStatementId, true);
        if (null != sqlSegment) {
            plainSelect.setWhere(sqlSegment);
        }
    }

}

