package org.ruoyi.common.wechat.web.constant;

import com.jfinal.kit.PathKit;

import java.io.File;

public class UploadConstant {

    public static final String IMG_FOLD = "img";
    public static final String FILE_FOLD = "file";

    public static final String IMG_URL = "/"+IMG_FOLD+"/";
    public static final String FILE_URL = "/"+FILE_FOLD+"/";

    public static final String IMG_PATH = PathKit.getWebRootPath()+ File.separator +IMG_FOLD;
    public static final String FILE_PATH = PathKit.getWebRootPath()+ File.separator +FILE_FOLD;
    public static final String IMG_PATH_SEP = IMG_PATH + File.separator;
    public static final String FILE_PATH_SEP = FILE_PATH + File.separator;


}
