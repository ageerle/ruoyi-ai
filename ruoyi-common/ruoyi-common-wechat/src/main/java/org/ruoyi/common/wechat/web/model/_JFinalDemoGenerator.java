package org.ruoyi.common.wechat.web.model;

import com.jfinal.kit.PathKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.activerecord.generator.Generator;
import com.jfinal.plugin.druid.DruidPlugin;
import org.ruoyi.common.wechat.web.common.MyConfig;

import javax.sql.DataSource;

/**
 * 本 demo 仅表达最为粗浅的 jfinal 用法，更为有价值的实用的企业级用法
 * 详见 JFinal 俱乐部: http://jfinal.com/club
 *
 * 在数据库表有任何变动时，运行一下 main 方法，极速响应变化进行代码重构
 */
public class _JFinalDemoGenerator {

//	public static DataSource getDataSource() {
////		PropKit.use("appConfig.properties");
////		DruidPlugin druidPlugin =  new DruidPlugin(PropKit.get("jdbcUrl"), PropKit.get("user"), PropKit.get("password").trim(),PropKit.get("jdbcDriverSqlServe"));
////		DruidPlugin druidPlugin = new DruidPlugin("jdbc:mysql://127.0.0.1:3306/wxwobot?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull","root", "root");
//		DruidPlugin druidPlugin = new DruidPlugin("jdbc:mysql://localhost:3306/wxwobot?characterEncoding=utf8&useSSL=false&zeroDateTimeBehavior=convertToNull",
//				"root", "root");
//		druidPlugin.start();
//		return druidPlugin.getDataSource();
//	}
	public static DataSource getDataSource() {
		DruidPlugin druidPlugin = MyConfig.createDruidPlugin();
		druidPlugin.start();
		return druidPlugin.getDataSource();
	}

	public static void main(String[] args) {
		PropKit.use("appConfig.properties");
		// base model 所使用的包名
		String baseModelPackageName = "org.ruoyi.common.wechat.web.model.base";
		// base model 文件保存路径
		String baseModelOutputDir = PathKit.getWebRootPath() + "/src/main/java/org.ruoyi.common.wechat/web/model/base";

		// model 所使用的包名 (MappingKit 默认使用的包名)
		String modelPackageName = "org.ruoyi.common.wechat.web.model";
		// model 文件保存路径 (MappingKit 与 DataDictionary 文件默认保存路径)
		String modelOutputDir = baseModelOutputDir + "/..";

		// 创建生成器
		Generator generator = new Generator(getDataSource(), baseModelPackageName, baseModelOutputDir, modelPackageName, modelOutputDir);
		// 设置是否生成链式 setter 方法
		generator.setGenerateChainSetter(false);
		// 添加不需要生成的表名
		generator.addExcludedTable();
		// 设置是否在 Model 中生成 dao 对象
		generator.setGenerateDaoInModel(false);
		// 设置是否生成链式 setter 方法
		generator.setGenerateChainSetter(true);
		// 设置是否生成字典文件
		generator.setGenerateDataDictionary(false);
		// 设置需要被移除的表名前缀用于生成modelName。例如表名 "osc_user"，移除前缀 "osc_"后生成的model名为 "User"而非 OscUser
//		generator.setRemovedTableNamePrefixes("t_");

		generator.setDialect(new MysqlDialect());
		// 生成
		generator.generate();

	}

}




