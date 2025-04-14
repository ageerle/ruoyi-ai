package org.ruoyi.rocket.callmcpserver.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author jianzhang
 * 2025/03/18/下午8:00
 */
@Controller
public class IndexController {

    @GetMapping("/")
    public String chat(Model model) {
        //model.addAttribute("name", "User");
        // 返回视图名称，对应 templates/index.html
        return "index";
    }

}
