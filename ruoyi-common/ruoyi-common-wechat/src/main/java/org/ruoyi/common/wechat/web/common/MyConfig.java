package org.ruoyi.common.wechat.web.common;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.config.*;
import com.jfinal.json.FastJsonFactory;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.server.undertow.UndertowServer;
import com.jfinal.template.Engine;
import com.jfinal.template.source.ClassPathSourceFactory;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.web.constant.UploadConstant;
import org.ruoyi.common.wechat.web.interceptor.ExceptionInterceptor;
import org.ruoyi.common.wechat.web.model._MappingKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * JFinal项目的核心配置
 * 详情查看官方文档
 * https://www.jfinal.com/doc
 *
 * @author WesleyOne
 */
public class MyConfig extends JFinalConfig {

	public final Logger LOG = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args) {
		UndertowServer.start(MyConfig.class, 8180, true);
	}

	/**
	 * 配置常量
	 */
	@Override
	public void configConstant(Constants me) {
		PropKit.use("appConfig.properties");
		me.setDevMode(PropKit.getBoolean("devMode", false));
		//上传的文件的最大50M
		me.setMaxPostSize(10 * 1024 * 1024);
		me.setEncoding("UTF-8");
		me.setJsonFactory(new FastJsonFactory());
		me.setError404View("/WEB-INF/templates/404.html");
	}

	/**
	 * 配置路由
	 */
	@Override
	public void configRoute(Routes me) {
		me.add(new MyRoute());
		me.add(new OutRoute());
	}

	@Override
	public void configEngine(Engine me) {
		me.setDevMode(PropKit.use("appConfig.properties").getBoolean("devMode", false));
		me.addSharedFunction("/WEB-INF/templates/bs4temp/layout.html");
		me.addSharedObject("imgDomain" , UploadConstant.IMG_URL);
		me.addSharedObject("filedomain" , UploadConstant.FILE_URL);
	}

	/**
	 * 配置插件
	 */
	@Override
	public void configPlugin(Plugins me) {
		// 配置 druid 数据库连接池插件
		DruidPlugin druidPlugin = createDruidPlugin();
		druidPlugin.addFilter(new StatFilter());
		WallFilter wall = new WallFilter();
		wall.setDbType("mysql");
		druidPlugin.addFilter(wall);
		druidPlugin.setInitialSize(1);
		me.add(druidPlugin);

		// 配置ActiveRecord插件
		ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
		_MappingKit.mapping(arp);
		arp.setDialect(new MysqlDialect());
		arp.setShowSql(PropKit.use("appConfig.properties").getBoolean("devMode", false));
		arp.getEngine().setSourceFactory(new ClassPathSourceFactory());
		me.add(arp);
	}

	public static DruidPlugin createDruidPlugin() {
		return new DruidPlugin(PropKit.get("jdbcUrl"), PropKit.get("user"), PropKit.get("password").trim());
	}
	/**
	 * 配置全局拦截器
	 */
	@Override
	public void configInterceptor(Interceptors me) {
		me.add(new ExceptionInterceptor());
	}

	/**
	 * 配置处理器
	 */
	@Override
	public void configHandler(Handlers me) {
	}

	@Override
	public void afterJFinalStart() {
		System.setProperty("jsse.enableSNIExtension", "false");
		// 检查文件夹(/热登录/下载根目录)是否存在
        checkFileExist();

		// 热登陆操作
		CoreManage.reload();
	}

    @Override
	public void beforeJFinalStop() {
		CoreManage.persistence();
	}

    /**
     * 检查文件夹(/热登录/下载根目录)是否存在
     */
    private void checkFileExist() {
        String hotReloadDir = PropKit.get("hotReloadDir");
        String downloadPath = PropKit.get("download_path");
        String logPath = PropKit.get("log_path");
        File hotReloadFile = new File(hotReloadDir);
        if (!hotReloadFile.exists()){
            if (!hotReloadFile.mkdirs()) {
                LOG.error("热加载文件夹创建失败[{}]",hotReloadDir);
            }
        }
        File downloadFile = new File(downloadPath);
        if (!downloadFile.exists()){
            if (!downloadFile.mkdirs()) {
                LOG.error("下载文件夹创建失败[{}]",downloadPath);
            }
        }
        File logFile = new File(logPath);
        if (!logFile.exists()){
            if (!logFile.mkdirs()) {
                LOG.error("日志文件夹创建失败[{}]",logPath);
            }
        }
    }

}
