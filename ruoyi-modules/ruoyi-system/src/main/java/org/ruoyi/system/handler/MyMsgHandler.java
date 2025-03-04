package org.ruoyi.system.handler;

import com.jfinal.kit.PropKit;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.images.Item;
import org.ruoyi.common.wechat.itchat4j.beans.BaseMsg;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.itchat4j.face.IMsgHandlerFace;
import org.ruoyi.common.wechat.itchat4j.utils.LogInterface;
import org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType;
import org.ruoyi.common.wechat.itchat4j.utils.tools.CommonTools;
import org.ruoyi.common.wechat.web.constant.ConfigKeys;
import org.ruoyi.common.wechat.web.model.WxRobConfig;
import org.ruoyi.common.wechat.web.model.WxRobKeyword;
import org.ruoyi.system.domain.vo.WxRobConfigVo;
import org.ruoyi.system.service.ISseService;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 消息处理实现 默认方案
 * @author WesleyOne
 * @create 2018/12/11
 */
public class MyMsgHandler implements IMsgHandlerFace,LogInterface {
    private String uniqueKey;
    private final ISseService sseService;
    private WxRobConfigVo robConfigVo;

    public MyMsgHandler(String uniqueKey,ISseService sseService,WxRobConfigVo robConfigVo){
        this.uniqueKey = uniqueKey;
        this.sseService = sseService;
        this.robConfigVo = robConfigVo;
    }

    private String getDownloadPath(String fileName) {
        String download_path = PropKit.get("download_path");
        return download_path+ File.separator + uniqueKey +File.separator + fileName;
    }

    @Override
    public void textMsgHandle(BaseMsg msg) {
        String fromNickName = msg.getFromNickName();
        String fromUserName = msg.getFromUserName();
        boolean groupMsg = msg.isGroupMsg();
        String text = msg.getText().trim();

        if (groupMsg && text.contains("@" + fromNickName)) {
            handleGroupMessage(fromNickName, fromUserName, text);
        } else {
            respondWithChatGPT(fromUserName, text);
        }
    }

    private void handleGroupMessage(String fromNickName, String fromUserName, String text) {
        String prompt = text.replace("@" + fromNickName + " ", "");
        if (prompt.startsWith("画")) {
            generateAndSendImage(fromUserName, prompt.replaceFirst("画 ", ""));
        } else {
            respondWithChatGPT(fromUserName, prompt);
        }
    }

    private void generateAndSendImage(String fromUserName, String prompt) {
        List<Item> items = sseService.wxDall(prompt,robConfigVo.getUserId().toString());
        WxRobKeyword robKeyword = new WxRobKeyword();
        robKeyword.setTypeData(SendMsgType.IMG.toValue());
        robKeyword.setValueData(items.get(0).getUrl());
        sendDataByType(fromUserName, robKeyword);
    }



    private void respondWithChatGPT(String fromUserName, String prompt) {
        ChatRequest chatBO = new ChatRequest();
        //chatBO.setPrompt(prompt);
        chatBO.setModel(ChatCompletion.Model.GPT_3_5_TURBO.getName());
        String chat = sseService.chat(chatBO,robConfigVo.getUserId().toString());
        WxRobKeyword robKeyword = new WxRobKeyword();
        robKeyword.setTypeData(SendMsgType.TEXT.toValue());
        robKeyword.setValueData(chat);
        sendDataByType(fromUserName, robKeyword);
    }

    private boolean sendDataByType(String fromUserName, WxRobKeyword robKeyword) {
        String data;
        String type;
        if (robKeyword != null){
            data = robKeyword.getValueData();
            type = robKeyword.getTypeData();
            CoreManage.addSendMsg4UserName(uniqueKey,fromUserName,data,SendMsgType.fromValue(type));
            return true;
        }
        return false;
    }


    @Override
    public void sysMsgHandle(BaseMsg msg) {
        String fromNickName = msg.getFromNickName();
        String fromUserName = msg.getFromUserName();
        boolean groupMsg = msg.isGroupMsg();

        /**
         * 群里的新人进群消息处理
         * 优先发专门这个群的欢迎词
         * 没有发通用的
         * 欢迎词内容实质就是在最前面加上@昵称\n
         *
         * 欢迎词的关键字
         * @see ConfigKeys#DEAFAULT_WELCOME
         */

        // 解析新人名字
        String text = msg.getContent();
        String newNickName = "";
        Matcher matcher = CommonTools.getMatcher("邀请\"(.+?)\"加入了群聊", text);
        if (matcher.find()){
            newNickName = matcher.group(1);
        }else{
            matcher = CommonTools.getMatcher("\"(.+?)\"通过扫描(.+?)分享的二维码加入群聊", text);
            if (matcher.find()){
                newNickName = matcher.group(1);
            }
        }

        if (StringUtils.isNotEmpty(newNickName)){

            WxRobConfig robConfig = WxRobConfig.dao.findFirst("SELECT * FROM wx_rob_config WHERE unique_key = ? LIMIT 1", uniqueKey);
            if(robConfig != null && robConfig.getEnable()){
                // 判断是否要回复
                boolean isOpen = false;
                // 判断是群聊的话是否允许回复 昵称关键字
                if (robConfig.getToGroup() && groupMsg){
                    isOpen = true;
                }
                if (isOpen){
                    WxRobKeyword robKeyword = WxRobKeyword.dao.findFirst("SELECT * FROM wx_rob_keyword WHERE unique_key = ? AND key_data = ? AND nick_name = ? AND enable = 1 AND to_group = ? ORDER BY id DESC LIMIT 1", uniqueKey, ConfigKeys.DEAFAULT_WELCOME,fromNickName,msg.isGroupMsg()?1:0);
                    if (sendSysWelcomeMsg(fromUserName, newNickName, robKeyword)){ return;}
                }

                // 没有专门的关键字，则使用默认关键字
                isOpen = false;
                // 判断是群聊的话是否允许回复 昵称关键字
                if (robConfig.getDefaultGroup() && groupMsg){
                    isOpen = true;
                }
                if (isOpen){
                    WxRobKeyword defaultRobKeyword = WxRobKeyword.dao.findFirst("SELECT * FROM wx_rob_keyword WHERE unique_key = ? AND key_data = ? AND nick_name = ? AND enable = 1 AND to_group = ? ORDER BY id DESC LIMIT 1", uniqueKey, ConfigKeys.DEAFAULT_WELCOME, ConfigKeys.DEAFAULT_KEYWORD,msg.isGroupMsg()?1:0);
                    if (sendSysWelcomeMsg(fromUserName, newNickName, defaultRobKeyword)){ return;}
                }
            }
        }

    }

    /**
     * 发送欢迎内容
     * @param fromUserName
     * @param newNickName
     * @param robKeyword
     * @return
     */
    private boolean sendSysWelcomeMsg(String fromUserName, String newNickName, WxRobKeyword robKeyword) {
        if (robKeyword != null){
            if (robKeyword.getTypeData().equals(SendMsgType.TEXT.toValue())){
                robKeyword.setValueData(String.format("@%s\n%s",newNickName,robKeyword.getValueData()));
            }
            if (sendDataByType(fromUserName, robKeyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void picMsgHandle(BaseMsg msg) {
//        // 这里使用收到图片的时间作为文件名
//        String fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".jpg";
//        // 保存图片的路径
//        String picPath = getDownloadPath(fileName);
//        // 调用此方法来保存图片
//        DownloadTools.getDownloadFn(msg, MsgTypeEnum.PIC.getType(), picPath, this.uniqueKey);
//        CoreManage.addSendMsg4UserName(uniqueKey,msg.getFromUserName(),"图片保存成功",SendMsgType.TEXT);
        return;
    }

    @Override
    public void voiceMsgHandle(BaseMsg msg) {
//        // 这里使用收到语音的时间作为文件名
//        String fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".mp3";
//        // 保存语音的路径
//        String voicePath = getDownloadPath(fileName);
//        // 调用此方法来保存语音
//        DownloadTools.getDownloadFn(msg, MsgTypeEnum.VOICE.getType(), voicePath, this.uniqueKey);
//        CoreManage.addSendMsg4UserName(uniqueKey,msg.getFromUserName(),"声音保存成功",SendMsgType.TEXT);
        return;
    }

    @Override
    public void videoMsgHandle(BaseMsg msg) {
//        // 这里使用收到小视频的时间作为文件名
//        String fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".mp4";
//        // 保存小视频的路径
//        String viedoPath = getDownloadPath(fileName);
//        // 调用此方法来保存小视频
//        DownloadTools.getDownloadFn(msg, MsgTypeEnum.VIEDO.getType(), viedoPath,this.uniqueKey);
//        CoreManage.addSendMsg4UserName(uniqueKey,msg.getFromUserName(),"视频保存成功",SendMsgType.TEXT);
        return;
    }

    @Override
    public void nameCardMsgHandle(BaseMsg msg) {
        return ;
    }

    @Override
    public void verifyAddFriendMsgHandle(BaseMsg msg) {
        return ;
    }

    @Override
    public void mediaMsgHandle(BaseMsg msg) {
        return ;
    }
}
