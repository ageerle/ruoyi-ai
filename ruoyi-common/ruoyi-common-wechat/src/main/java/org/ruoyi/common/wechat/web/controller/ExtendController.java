package org.ruoyi.common.wechat.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType;
import org.ruoyi.common.wechat.web.model.WxRobRelation;
import org.ruoyi.common.wechat.web.utils.IpUtil;

/**
 * 对外接口
 * @author WesleyOne
 * @create 2018/12/16
 */
public class ExtendController extends _BaseController {


    /**
     * 对外通用规则发消息
     * 参数:
     * ok       外接唯一码
     * msg     消息列表
     *         类型参考@see     org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType
     *          TEXT     文本消息串
     *          IMG      图片名串（需要后台-通用工具-上传获取）
     *          FILE     文件名串（需要后台-通用工具-上传获取）
     *  例子:
     *  {"ok":"test123",
     *   "msg":[
     *       {"type":"TEXT","body":"我是消息体"},
     *       {"type":"IMG","body":"ty6yLk3X_1545142908614.jpg"},
     *       {"type":"FILE","body":"ty6yLk3X_1545142537914.txt"},
     *   ]
     *  }
     *
     *  说明:
     *      发送顺序按照列表顺序从前往后发
     *
     * 返回:
     * 00   成功
     * 01   外接码不存在
     * 02   外接码失效
     * 03   IP未通过审核
     *
     */
    public void sendMsg(){

        JSONObject postParam = getPostParam();
        String outKey = postParam.getString("ok");
        String msgStr = postParam.getString("msg");
        JSONArray msgs = JSONArray.parseArray(msgStr);

        if (StringUtils.isEmpty(outKey)){
            setCode("01");
            setMsg("外接码不存在");
            renderJson();
            return;
        }

        WxRobRelation relationRecord = WxRobRelation.dao.findFirst("SELECT * FROM wx_rob_relation WHERE out_key = ? LIMIT 1", outKey);

        /**
         * 校验IP
         * 1.配置空则拒绝所有
         * 2.存在0.0.0.0不校验
         */
        String whiteList = relationRecord.getWhiteList();
        if (StringUtils.isEmpty(whiteList) || !relationRecord.getEnable()){
            setCode("02");
            setMsg("外接码失效或未配置白名单");
            renderJson();
            return;
        }
        String allPassIp = "0.0.0.0";
        // 不是完全开放IP并且访问IP不存在
        if (!whiteList.contains(allPassIp)){
            String outRealIp = IpUtil.getRealIp(getRequest());
            if (!whiteList.contains(outRealIp)){
                setCode("03");
                setMsg("IP未通过审核");
                renderJson();
                return;
            }
        }

        String uniqueKey = relationRecord.getUniqueKey();
        // 查看机器是否加载完成
        if (!CoreManage.getInstance(uniqueKey).isAlive() || !CoreManage.getInstance(uniqueKey).isFinishInit()){
            setCode("05");
            setMsg("机器未准备完成");
            renderJson();
            return;
        }


        /**
         * 添加到消息队列
         */
        Boolean toGroup = relationRecord.getToGroup();
        String nickName = relationRecord.getNickName();
        // 单次请求最大消息数
        int maxMessages = 10;
        int msgLength = msgs.size();
        if (msgLength<maxMessages){
            maxMessages = msgLength;
        }
        // 到这一步默认返回成功
        boolean result = true;
        for (int i=0;i<maxMessages;i++){
            JSONObject message = msgs.getJSONObject(i);
            String type = message.getString("type");
            String body = message.getString("body");

            CoreManage.addSendMsg4NickName(uniqueKey,nickName,body,SendMsgType.fromValue(type),toGroup);
        }

        if (!result){
            setOperateErr();
        }

        renderJson();
    }

}
