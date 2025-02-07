package org.ruoyi.common.core.utils.ip;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.File;

/**
 * 根据ip地址定位工具类，离线方式
 * 参考地址：<a href="https://gitee.com/lionsoul/ip2region/tree/master/binding/java">集成 ip2region 实现离线IP地址定位库</a>
 *
 * @author lishuyan
 */
@Slf4j
public class RegionUtils {

    private static final Searcher SEARCHER;

    static {
        String fileName = "/ip2region.xdb";
        File existFile = FileUtils.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
        if (!FileUtils.exist(existFile)) {
            ClassPathResource fileStream = new ClassPathResource(fileName);
            if (ObjectUtil.isEmpty(fileStream.getStream())) {
                throw new ServiceException("RegionUtils初始化失败，原因：IP地址库数据不存在！");
            }
            FileUtils.writeFromStream(fileStream.getStream(), existFile);
        }

        String dbPath = existFile.getPath();

        // 1、从 dbPath 加载整个 xdb 到内存。
        byte[] cBuff;
        try {
            cBuff = Searcher.loadContentFromFile(dbPath);
        } catch (Exception e) {
            throw new ServiceException("RegionUtils初始化失败，原因：从ip2region.xdb文件加载内容失败！" + e.getMessage());
        }
        // 2、使用上述的 cBuff 创建一个完全基于内存的查询对象。
        try {
            SEARCHER = Searcher.newWithBuffer(cBuff);
        } catch (Exception e) {
            throw new ServiceException("RegionUtils初始化失败，原因：" + e.getMessage());
        }
    }

    /**
     * 根据IP地址离线获取城市
     */
    public static String getCityInfo(String ip) {
        try {
            ip = ip.trim();
            // 3、执行查询
            String region = SEARCHER.search(ip);
            return region.replace("0|", "").replace("|0", "");
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", ip);
            return "未知";
        }
    }

}
