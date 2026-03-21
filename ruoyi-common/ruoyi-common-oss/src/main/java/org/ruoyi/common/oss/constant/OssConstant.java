package org.ruoyi.common.oss.constant;

import org.ruoyi.common.core.constant.GlobalConstants;

import java.util.Arrays;
import java.util.List;

/**
 * 对象存储常量
 *
 * @author Lion Li
 */
public interface OssConstant {

    /**
     * 默认配置KEY
     */
    String DEFAULT_CONFIG_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "sys_oss:default_config";

    /**
     * 预览列表资源开关Key
     */
    String PEREVIEW_LIST_RESOURCE_KEY = "sys.oss.previewListResource";

    /**
     * 系统数据ids
     */
    List<Long> SYSTEM_DATA_IDS = Arrays.asList(1L, 2L, 3L, 4L);

    /**
     * 云服务商
     */
    String[] CLOUD_SERVICE = new String[] {"aliyun", "qcloud", "qiniu", "obs"};

    /**
     * https 状态
     */
    String IS_HTTPS = "Y";

    // 文档解析前缀
    String FILE_ID_PREFIX = "fileid://";

    // 服务名称
    String DASH_SCOPE = "Qwen";

    // apiKey 配置名称
    String CONFIG_NAME_KEY = "file.api.key";

    // apiHost 配置名称
    String CONFIG_NAME_URL = "file.api.host";

}
