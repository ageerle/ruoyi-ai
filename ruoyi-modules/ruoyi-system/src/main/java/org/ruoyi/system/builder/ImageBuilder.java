package org.ruoyi.system.builder;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutImageMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;


/**
 *  @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
public class ImageBuilder extends AbstractBuilder {

  @Override
  public WxCpXmlOutMessage build(String content, WxCpXmlMessage wxMessage,
                                 WxCpService service) {

    WxCpXmlOutImageMessage m = WxCpXmlOutMessage.IMAGE().mediaId(content)
        .fromUser(wxMessage.getToUserName()).toUser(wxMessage.getFromUserName())
        .build();

    return m;
  }

}
