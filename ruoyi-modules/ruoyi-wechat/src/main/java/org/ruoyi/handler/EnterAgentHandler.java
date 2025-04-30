package org.ruoyi.handler;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;

import java.util.Map;

/**
 * <pre>
 *
 * Created by Binary Wang on 2018/8/27.
 * </pre>
 *
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@Slf4j
public class EnterAgentHandler extends AbstractHandler {
    private static final int TEST_AGENT = 1000002;

    @Override
    public WxCpXmlOutMessage handle(WxCpXmlMessage wxMessage, Map<String, Object> context, WxCpService wxCpService, WxSessionManager sessionManager) throws WxErrorException {
        // do something
        return null;
    }
}
