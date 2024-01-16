package com.xmzs.demo.controller;

import com.xmzs.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * swagger3 用法示例
 *
 * @author Lion Li
 */
@RestController
@RequestMapping("/swagger/demo")
public class Swagger3DemoController {

    /**
     * 上传请求
     * 必须使用 @RequestPart 注解标注为文件
     *
     * @param file 文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        return R.ok("操作成功", file.getOriginalFilename());
    }

}
