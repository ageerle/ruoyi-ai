package org.ruoyi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页
 *
 * @author Lion Li
 */
@SaIgnore
@RequiredArgsConstructor
@Controller
public class IndexController {

    /**
     * 访问首页，提示语
     */
    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/success")
    public String success(){
        return "paySuccess.html";
    }

    @GetMapping("/cancel")
    public String cancel(){
        return "cancel";
    }

}
