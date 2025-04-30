package org.ruoyi.builder;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutTextMessage;

/**
 *  @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
public class TextBuilder extends AbstractBuilder {

  @Override
  public WxCpXmlOutMessage build(String content, WxCpXmlMessage wxMessage,
                                 WxCpService service) {
    WxCpXmlOutTextMessage m = WxCpXmlOutMessage.TEXT().content(content)
        .fromUser(wxMessage.getToUserName()).toUser(wxMessage.getFromUserName())
        .build();
    return m;
  }

}
