package org.ruoyi.common.core.utils.ip;

import cn.hutool.core.io.resource.NoResourceException;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;

/**
 * 根据ip地址定位工具类，离线方式
 * 参考地址：<a href="https://gitee.com/lionsoul/ip2region/tree/master/binding/java">集成 ip2region 实现离线IP地址定位库</a>
 *
 * @author lishuyan
 */
@Slf4j
public class RegionUtils {

    // IP地址库文件名称
    public static final String IP_XDB_FILENAME = "ip2region.xdb";

    private static final Searcher SEARCHER;

    static {
        try {
            // 1、将 ip2region 数据库文件 xdb 从 ClassPath 加载到内存。
            // 2、基于加载到内存的 xdb 数据创建一个 Searcher 查询对象。
            SEARCHER = Searcher.newWithBuffer(ResourceUtil.readBytes(IP_XDB_FILENAME));
            log.info("RegionUtils初始化成功，加载IP地址库数据成功！");
        } catch (NoResourceException e) {
            throw new ServiceException("RegionUtils初始化失败，原因：IP地址库数据不存在！");
        } catch (Exception e) {
            throw new ServiceException("RegionUtils初始化失败，原因：" + e.getMessage());
        }
    }

    /**
     * 根据IP地址离线获取城市
     */
    public static String getCityInfo(String ip) {
        try {
            // 3、执行查询
            String region = SEARCHER.search(StringUtils.trim(ip));
            return region.replace("0|", "").replace("|0", "");
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", ip);
            return "未知";
        }
    }

}
