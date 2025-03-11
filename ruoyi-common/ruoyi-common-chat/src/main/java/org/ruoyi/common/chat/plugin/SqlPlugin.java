package org.ruoyi.common.chat.plugin;

import org.ruoyi.common.chat.openai.plugin.PluginAbstract;

import java.sql.*;

/**
 * @author ageer
 */
public class SqlPlugin extends PluginAbstract<SqlReq, SqlResp> {

    public SqlPlugin(Class<?> r) {
        super(r);
    }



    @Override
    public SqlResp func(SqlReq args) {
        SqlResp resp = new SqlResp();
        resp.setUserBalance(getBalance(args.getUsername()));
        return resp;
    }

    @Override
    public String content(SqlResp resp) {
        return  "用户余额："+resp.getUserBalance();
    }


    public String getBalance(String userName) {
        // MySQL 8.0 以下版本 - JDBC 驱动名及数据库 URL
        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        String DB_URL = "jdbc:mysql://43.139.70.230:3306/ry-vue";
        // 数据库的用户名与密码，需要根据自己的设置
        String USER = "ry-vue";
        String PASS = "BXZiGsY35K523Xfx";
        Connection conn = null;
        Statement stmt = null;
        String balance = "0.1";

        try{
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);

            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);

            // 执行查询
            System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT user_balance FROM sys_user where user_name ='" + userName + "'";
            ResultSet rs = stmt.executeQuery(sql);
            // 展开结果集数据库
            while(rs.next()){
                // 通过字段检索
                balance = rs.getString("user_balance");
                // 输出数据
                System.out.print("余额: " + balance);
                System.out.print("\n");
            }
            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
        }catch(SQLException se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }finally{
            // 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException se2){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
       return balance;
    }
}
