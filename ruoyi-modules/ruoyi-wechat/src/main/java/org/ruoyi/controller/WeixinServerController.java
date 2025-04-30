package org.ruoyi.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.service.WeixinUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author https://www.wdbyte.com
 */
@Slf4j
@RestController
public class WeixinServerController {

    @Autowired
    private WeixinUserService weixinUserService;

    @GetMapping(value = "/weixin/check")
    public String weixinCheck(HttpServletRequest request) {
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        String echostr = request.getParameter("echostr");

        if (StringUtils.isEmpty(signature) || StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(nonce)) {
            return "";
        }
        weixinUserService.checkSignature(signature, timestamp, nonce);
        return echostr;
    }

    @PostMapping(value = "/weixin/check")
    public String weixinMsg(@RequestBody String requestBody, @RequestParam("signature") String signature,
        @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce) {

        log.debug("requestBody:{}", requestBody);
        log.debug("signature:{}", signature);
        log.debug("timestamp:{}", timestamp);
        log.debug("nonce:{}", nonce);

        weixinUserService.checkSignature(signature, timestamp, nonce);
        return weixinUserService.handleWeixinMsg(requestBody);
    }

}
