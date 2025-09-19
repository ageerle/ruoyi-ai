package org.ruoyi.builder;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;

/**
 *  @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
public abstract class AbstractBuilder {
  public abstract WxCpXmlOutMessage build(String content, WxCpXmlMessage wxMessage, WxCpService service);
}
