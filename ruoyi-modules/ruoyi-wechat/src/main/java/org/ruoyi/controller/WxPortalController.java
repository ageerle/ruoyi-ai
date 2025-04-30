package org.ruoyi.controller;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.utils.JsonUtils;

import org.ruoyi.config.WxCpConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@RestController
@RequestMapping("/wx/cp")
@Slf4j
public class WxPortalController {

    @Value("${wechat.cp.appConfigs[0].agentId}")
    private Integer agentId;


  @GetMapping(produces = "text/plain;charset=utf-8")
  public String authGet(
                        @RequestParam(name = "msg_signature", required = false) String signature,
                        @RequestParam(name = "timestamp", required = false) String timestamp,
                        @RequestParam(name = "nonce", required = false) String nonce,
                        @RequestParam(name = "echostr", required = false) String echostr) {
    log.info("\n接收到来自微信服务器的认证消息：signature = [{}], timestamp = [{}], nonce = [{}], echostr = [{}]",
        signature, timestamp, nonce, echostr);

    if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
      throw new IllegalArgumentException("请求参数非法，请核实!");
    }

    final WxCpService wxCpService = WxCpConfiguration.getCpService(agentId);
    if (wxCpService == null) {
      throw new IllegalArgumentException(String.format("未找到对应agentId=[%d]的配置，请核实！", agentId));
    }

    if (wxCpService.checkSignature(signature, timestamp, nonce, echostr)) {
      return new WxCpCryptUtil(wxCpService.getWxCpConfigStorage()).decrypt(echostr);
    }

    return "非法请求";
  }

  @PostMapping(produces = "application/xml; charset=UTF-8")
  public String post(
                     @RequestBody String requestBody,
                     @RequestParam("msg_signature") String signature,
                     @RequestParam("timestamp") String timestamp,
                     @RequestParam("nonce") String nonce) {
    log.info("\n接收微信请求：[signature=[{}], timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
        signature, timestamp, nonce, requestBody);

    final WxCpService wxCpService = WxCpConfiguration.getCpService(1000002);
    WxCpXmlMessage inMessage = WxCpXmlMessage.fromEncryptedXml(requestBody, wxCpService.getWxCpConfigStorage(),
        timestamp, nonce, signature);
    log.debug("\n消息解密后内容为：\n{} ", JsonUtils.toJson(inMessage));
    WxCpXmlOutMessage outMessage = this.route(1000002, inMessage);
    if (outMessage == null) {
      return "";
    }

    String out = outMessage.toEncryptedXml(wxCpService.getWxCpConfigStorage());
    log.debug("\n组装回复信息：{}", out);
    return out;
  }

  private WxCpXmlOutMessage route(Integer agentId, WxCpXmlMessage message) {
    try {
      return WxCpConfiguration.getRouters().get(agentId).route(message);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }


}
