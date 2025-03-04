package org.ruoyi.common.wechat.itchat4j.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ruoyi.common.wechat.itchat4j.beans.BaseMsg;
import org.ruoyi.common.wechat.itchat4j.beans.Member;
import org.ruoyi.common.wechat.itchat4j.beans.WebWxInit;
import org.ruoyi.common.wechat.itchat4j.core.Core;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.itchat4j.core.MsgCenter;
import org.ruoyi.common.wechat.itchat4j.service.ILoginService;
import org.ruoyi.common.wechat.itchat4j.utils.LogInterface;
import org.ruoyi.common.wechat.itchat4j.utils.SleepUtils;
import org.ruoyi.common.wechat.itchat4j.utils.enums.*;
import org.ruoyi.common.wechat.itchat4j.utils.enums.parameters.BaseParaEnum;
import org.ruoyi.common.wechat.itchat4j.utils.enums.parameters.LoginParaEnum;
import org.ruoyi.common.wechat.itchat4j.utils.enums.parameters.StatusNotifyParaEnum;
import org.ruoyi.common.wechat.itchat4j.utils.enums.parameters.UUIDParaEnum;
import org.ruoyi.common.wechat.itchat4j.utils.tools.CommonTools;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

/**
 * 登陆服务实现类
 *
 * @author https://github.com/yaphone
 * @author WesleyOne 修改
 * @version 1.0
 * @date 创建时间：2017年5月13日 上午12:09:35
 */
public class LoginServiceImpl implements ILoginService, LogInterface {
    private Core core;
    private String uniqueKey;

    public LoginServiceImpl(String uniqueKey) {
        this.uniqueKey = uniqueKey;
        this.core = CoreManage.getInstance(uniqueKey);
    }

    @Override
    public boolean login() {

        boolean isLogin = false;
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(LoginParaEnum.LOGIN_ICON.para(), LoginParaEnum.LOGIN_ICON.value()));
        params.add(new BasicNameValuePair(LoginParaEnum.UUID.para(), core.getUuid()));
        params.add(new BasicNameValuePair(LoginParaEnum.TIP.para(), LoginParaEnum.TIP.value()));

        // long time = 4000;
        long startMillis = System.currentTimeMillis();
        boolean overTime = false;

        while (!isLogin && !overTime) {
            // 防止请求重复时的问题
            if (core.isAlive()) {
                break;
            }

            long millis = System.currentTimeMillis();
            params.add(new BasicNameValuePair(LoginParaEnum.R.para(), String.valueOf(millis / 1579L)));
            params.add(new BasicNameValuePair(LoginParaEnum._1.para(), String.valueOf(millis)));
            HttpEntity entity = core.getMyHttpClient().doGet(URLEnum.LOGIN_URL.getUrl(), params, true, null);

            try {
                String result = EntityUtils.toString(entity);
                String status = checklogin(result);
                if (ResultEnum.SUCCESS.getCode().equals(status)) {
                    // 处理结果
                    if (processLoginInfo(result)) {
                        isLogin = true;
                        core.setAlive(isLogin);
                        break;
                    } else {
                        isLogin = false;
                        core.setAlive(isLogin);
                        // 登入异常直接退出，防止死循环
                        break;
                    }
                }
                if (ResultEnum.WAIT_CONFIRM.getCode().equals(status)) {
                    LOG.info("请点击微信确认按钮，进行登陆");
                }

            } catch (Exception e) {
                LOG.error("微信登陆异常！", e);
                break;
            }
            // 3分钟超时不再请求
            SleepUtils.sleep(1000);
            overTime = (millis - startMillis) > 1000 * 180;
        }
        return isLogin;
    }


    @Override
    public String getUuid() {
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(UUIDParaEnum.APP_ID.para(), UUIDParaEnum.APP_ID.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum.FUN.para(), UUIDParaEnum.FUN.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum.LANG.para(), UUIDParaEnum.LANG.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum._1.para(), String.valueOf(System.currentTimeMillis())));

        HttpEntity entity = core.getMyHttpClient().doGet(URLEnum.UUID_URL.getUrl(), params, true, null);

        try {
            String result = EntityUtils.toString(entity);
            String regEx = "window.QRLogin.code = (\\d+); window.QRLogin.uuid = \"(\\S+?)\";";
            Matcher matcher = CommonTools.getMatcher(regEx, result);
            LOG.info(result);
            if (matcher.find()) {
                if ((ResultEnum.SUCCESS.getCode().equals(matcher.group(1)))) {
                    core.setUuid(matcher.group(2));
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return core.getUuid();
    }

    @Deprecated
    @Override
    public boolean getQR(String qrPath) {
        return getQR(qrPath, false);
    }

    @Deprecated
    public boolean getQR(String qrPath, boolean open) {
        qrPath = qrPath + File.separator + "QR.jpg";
        String qrUrl = URLEnum.QRCODE_URL.getUrl() + core.getUuid();
        HttpEntity entity = core.getMyHttpClient().doGet(qrUrl, null, true, null);
        try {
            // 文件已存在先删除
            File file = new File(qrPath);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            // 写入二维码
            OutputStream out = new FileOutputStream(qrPath);
            byte[] bytes = EntityUtils.toByteArray(entity);
            out.write(bytes);
            out.flush();
            out.close();
            if (open) {
                try {
                    CommonTools.printQr(qrPath); // 打开登陆二维码图片
                } catch (Exception e) {
                    LOG.info(e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 获取自身登录信息缓存
     *
     * @return
     */
    @Override
    public boolean webWxInit() {
        core.setAlive(true);
        core.setLastNormalRetcodeTime(System.currentTimeMillis());

        try {
            // 组装请求URL和参数
            String url = String.format(URLEnum.INIT_URL.getUrl(),
                core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()),
                System.currentTimeMillis() / 3158L,
                core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));
            Map<String, Object> paramMap = core.getParamMap();

            // 请求初始化接口
            HttpEntity entity = core.getMyHttpClient().doPost(url, JSONObject.toJSONString(paramMap), getPersistentCookieMap());
            String result = EntityUtils.toString(entity, Consts.UTF_8);

            /**
             * 相关返回信息，本项目未做封装
             * @see WebWxInit
             */
            JSONObject obj = JSON.parseObject(result);

            JSONObject user = obj.getJSONObject(StorageLoginInfoEnum.User.getKey());
            JSONObject syncKey = obj.getJSONObject(StorageLoginInfoEnum.SyncKey.getKey());

            core.getLoginInfo().put(StorageLoginInfoEnum.InviteStartCount.getKey(),
                obj.getInteger(StorageLoginInfoEnum.InviteStartCount.getKey()));
            core.getLoginInfo().put(StorageLoginInfoEnum.SyncKey.getKey(), syncKey);

            JSONArray syncArray = syncKey.getJSONArray("List");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < syncArray.size(); i++) {
                sb.append(syncArray.getJSONObject(i).getString("Key") + "_"
                    + syncArray.getJSONObject(i).getString("Val") + "|");
            }
            // 1_661706053|2_661706420|3_661706415|1000_1494151022|
            String synckey = sb.toString();

            // 1_661706053|2_661706420|3_661706415|1000_1494151022
            core.getLoginInfo().put(StorageLoginInfoEnum.synckey.getKey(), synckey.substring(0, synckey.length() - 1));
            core.setUserName(user.getString("UserName"));
            core.setNickName(user.getString("NickName"));
            core.setUserSelf(obj.getJSONObject("User"));

            /**
             * TIP:
             * ContactList此处只是部分，不做处理，webwxgetcontact接口统一处理
             * MPSubscribeMsgList 公众号服务号也不处理
             */
        } catch (Exception e) {
            LOG.error("热登录出现该异常请忽略" + e.getMessage(), e);
            core.setAlive(false);
            return false;
        }
        return true;
    }

    @Override
    public void wxStatusNotify() {
        // 组装请求URL和参数
        String url = String.format(URLEnum.STATUS_NOTIFY_URL.getUrl(),
            core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));

        Map<String, Object> paramMap = core.getParamMap();
        paramMap.put(StatusNotifyParaEnum.CODE.para(), StatusNotifyParaEnum.CODE.value());
        paramMap.put(StatusNotifyParaEnum.FROM_USERNAME.para(), core.getUserName());
        paramMap.put(StatusNotifyParaEnum.TO_USERNAME.para(), core.getUserName());
        paramMap.put(StatusNotifyParaEnum.CLIENT_MSG_ID.para(), System.currentTimeMillis());
        String paramStr = JSON.toJSONString(paramMap);

        try {
            HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
//			String result = EntityUtils.toString(entity, Consts.UTF_8);
//			LOG.info(result);
        } catch (Exception e) {
            LOG.error("微信状态通知接口失败！", e);
        }

    }

    @Override
    public void startReceiving() {
        core.setAlive(true);
        Thread thread = new Thread(core.getThreadGroup(), new Runnable() {
            int retryCount = 0;

            @Override
            public void run() {
                while (core.isAlive()) {
                    try {
                        long startTime = System.currentTimeMillis();
                        Map<String, String> resultMap = syncCheck();
                        LOG.info(JSONObject.toJSONString(resultMap));
                        String retcode = resultMap.get("retcode");
                        String selector = resultMap.get("selector");
                        RetCodeEnum retCodeEnum = RetCodeEnum.fromCode(retcode);
                        if (retCodeEnum != null) {
                            LOG.info(retCodeEnum.getType());
                            if (retcode.equals(RetCodeEnum.UNKOWN.getCode())) {
                                // 防止频繁请求
                                Thread.sleep(1000);
                                continue;
                            } else if (retcode.equals(RetCodeEnum.SUCCESS.getCode())) {
                                // 修改最后收到正常报文时间
                                core.setLastNormalRetcodeTime(System.currentTimeMillis());
                                SelectorEnum selectorEnum = SelectorEnum.fromCode(selector);
                                if (selectorEnum != null) {
                                    if (selector.equals(SelectorEnum.NORMAL.getCode())) {
                                        continue;
                                    } else if (selector.equals(SelectorEnum.NEW_MSG.getCode())) {
                                        // 有新消息
//                                        processWebwxSync();
                                    } else if (selector.equals(SelectorEnum.ENTER_OR_LEAVE_CHAT.getCode())) {
//                                        processWebwxSync();
                                    } else if (selector.equals(SelectorEnum.MOD_CONTACT.getCode())) {
//                                        processWebwxSync();
                                    } else if (selector.equals(SelectorEnum.SELECTOR_3.getCode())) {
//                                        processWebwxSync();
//                                        continue;
                                    } else if (selector.equals(SelectorEnum.ADD_OR_DEL_CONTACT.getCode())) {
//                                        processWebwxSync();
                                    } else {
                                        LOG.error("UNKNOW SELECTOR CODE {}", selector);
                                    }
                                } else {
                                    // 防止新类型不处理堆积
//                                    processWebwxSync();
                                }
                            } else if (retcode.equals(RetCodeEnum.NOT_LOGIN_CHECK.getCode()) ||
                                retcode.equals(RetCodeEnum.TICKET_ERROR.getCode()) ||
                                retcode.equals(RetCodeEnum.PARAM_ERROR.getCode()) ||
                                retcode.equals(RetCodeEnum.NOT_LOGIN_WARN.getCode()) ||
                                retcode.equals(RetCodeEnum.COOKIE_INVALID_ERROR.getCode()) ||
                                retcode.equals(RetCodeEnum.LOGIN_ENV_ERROR.getCode())) {
                                // 状态异常直接退出
                                core.setAlive(false);
                                break;
                            } else {
                                // 防止频繁请求
                                Thread.sleep(1000);
                                break;
                            }
                        } else {
                            LOG.error("特殊retcode： {}", retcode);
                        }
                        // 统统尝试获取新消息
                        processWebwxSync();
                        if (System.currentTimeMillis() - startTime < 1000 * 1) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e0) {
                        LOG.error("线程中断");
                        core.setAlive(false);
                        break;
                    } catch (Exception e) {
                        LOG.error(e.getMessage());
                        retryCount += 1;
                        if (core.getReceivingRetryCount() < retryCount) {
                            core.setAlive(false);
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e1) {
                                LOG.error(e.getMessage());
                                break;
                            }
                        }
                    }

                }
            }
        }, "REC-" + uniqueKey);
        thread.start();
    }


    private void processWebwxSync() {
        JSONObject msgObj = webWxSync();
        if (msgObj != null) {
            Integer addMsgCount = msgObj.getInteger("AddMsgCount");
            Integer ModMsgCount = msgObj.getInteger("ModContactCount");
            Integer DelContactCount = msgObj.getInteger("DelContactCount");
            Integer ModChatRoomMemberCount = msgObj.getInteger("ModChatRoomMemberCount");


            if (addMsgCount > 0 || ModMsgCount > 0 || DelContactCount > 0 || ModChatRoomMemberCount > 0) {
                LOG.info("接收原文:{}", msgObj.toJSONString());
            }

            // 用于通知获取详细详细
            List<String> modUserName = new ArrayList<>();

            // 处理新消息
            try {

                if (addMsgCount > 0) {
                    JSONArray msgList = msgObj.getJSONArray("AddMsgList");
                    msgList = MsgCenter.produceMsg(msgList, uniqueKey);
                    for (int j = 0; j < msgList.size(); j++) {
                        BaseMsg baseMsg = JSON.toJavaObject(msgList.getJSONObject(j),
                            BaseMsg.class);
                        // TODO 日志
                        LOG.info("处理后对象:{}", JSON.toJSONString(baseMsg));
                        core.getMsgList().add(baseMsg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 处理修改联系人或群成员
            try {
                if (ModMsgCount > 0) {
                    JSONArray list = msgObj.getJSONArray("ModContactList");
                    int size = list.size();
                    for (int j = 0; j < size; j++) {
                        String userName = list.getJSONObject(j).getString("UserName");
                        if (userName.startsWith("@@")) {
                            CoreManage.addNewGroup(core, list.getJSONObject(j));
                        } else if (userName.startsWith("@")) {
                            CoreManage.addNewContact(core, list.getJSONObject(j));
                        }
                        modUserName.add(userName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            /**
             * 没有获取过数据，不知道干啥用
             * DelContactCount: 0
             * DelContactList: []
             * ModChatRoomMemberCount: 0
             * ModChatRoomMemberList: []
             */

            // 获取详细信息
            WebWxBatchGetContact(modUserName);

        }


    }


    @Override
    public void webWxGetContact() {
        String url = String.format(URLEnum.WEB_WX_GET_CONTACT.getUrl(),
            core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()));
        JSONArray member = new JSONArray();
        try {

            // 循环获取seq直到为0，即获取全部好友列表 ==0：好友获取完毕 >0：好友未获取完毕，此时seq为已获取的字节数
            Long seq = 0L;
            do {
                // 设置seq传参
                List<BasicNameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("r", String.valueOf(System.currentTimeMillis())));
                params.add(new BasicNameValuePair("seq", String.valueOf(seq)));
                params.add(new BasicNameValuePair("skey", core.getLoginInfo().get(BaseParaEnum.Skey.value()).toString()));
                HttpEntity entity = core.getMyHttpClient().doGet(url, params, false, getPersistentCookieMap());

                String result = EntityUtils.toString(entity, Consts.UTF_8);
                JSONObject fullFriendsJsonList = JSON.parseObject(result);

                if (fullFriendsJsonList.get("Seq") != null) {
                    seq = fullFriendsJsonList.getLong("Seq");
                }

                // 累加好友列表
                member.addAll(fullFriendsJsonList.getJSONArray(StorageLoginInfoEnum.MemberList.getKey()));
            } while (seq > 0);
            Iterator<?> iterator = member.iterator();
            while (iterator.hasNext()) {
                /**
                 * @see Member
                 */
                JSONObject o = (JSONObject) iterator.next();

                String userName = o.getString("UserName");

                if (StringUtils.isEmpty(userName)) {
                    LOG.error("{} 好友列表存在UserName空", core.getUniqueKey());
                    continue;
                }
                /**
                 * 自己信息不添加
                 */
                if (userName.equals(core.getUserName())) {
                    continue;
                }

                /**
                 * 开头@@           => 群聊
                 * 开头@            => VerifyFlag   ->  == 0    好友
                 *                                  -> != 0    公众号、服务号
                 * 不含@开头（其他） => 特殊账号
                 */

                if (userName.startsWith("@@")) {
                    // 群聊
                    CoreManage.addNewGroup(core, o);

                } else if (userName.startsWith("@")) {
                    Integer verifyFlag = o.getInteger("VerifyFlag");
                    if (verifyFlag != null && verifyFlag == 0) {
                        // 好友
                        CoreManage.addNewContact(core, o);
                    } else {
                        // 公众号
                        core.getPublicUsersList().add(o);
                    }
                } else {
                    // 特殊账号
                    core.getSpecialUsersList().add(o);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return;
    }

    /**
     * 获取群和好友详细信息
     * 首次加载用
     */
    @Override
    public void WebWxBatchGetContact() {

        Map<String, Object> paramMap = core.getParamMap();
        // 处理群成员信息
        int size = core.getGroupList().size();
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            HashMap<String, String> map = new HashMap<>(4);
            map.put("UserName", core.getGroupList().get(i).getString("UserName"));
            map.put("EncryChatRoomId", "");
            list.add(map);
        }
        // 处理玩家
        int sizeUser = core.getContactList().size();
        for (int i = 0; i < sizeUser; i++) {
            HashMap<String, String> map = new HashMap<>(4);
            map.put("UserName", core.getContactList().get(i).getString("UserName"));
            map.put("EncryChatRoomId", "");
            list.add(map);
        }

        WebWxBatchGetContactMain(paramMap, list);
    }

    /**
     * 获取群和好友详细信息
     * 过程中零散查询
     */
    public void WebWxBatchGetContact(List<String> userNameList) {

        if (CollectionUtil.isEmpty(userNameList)) {
            return;
        }

        String url = String.format(URLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
            core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()), System.currentTimeMillis(),
            core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = core.getParamMap();
        // 为了获取群成员信息
        List<Map<String, String>> list = new ArrayList<>();
        int size = userNameList.size();
        for (int i = 0; i < size; i++) {
            HashMap<String, String> map = new HashMap<>(4);
            map.put("UserName", userNameList.get(i));
            map.put("EncryChatRoomId", "");
            list.add(map);
        }
        WebWxBatchGetContactMain(paramMap, list);
    }

    private void WebWxBatchGetContactMain(Map<String, Object> paramMap, List<Map<String, String>> list) {

        String url = String.format(URLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
            core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()), System.currentTimeMillis(),
            core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));

        int totalSize = list.size();

        int batchSize = 50;
        int num = totalSize / batchSize;
        if (totalSize % batchSize > 0) {
            num += 1;
        }

        for (int i = 0; i < num; i++) {
            int startNum = i * batchSize;
            int endNum = (i + 1) * batchSize;
            if (endNum > totalSize) {
                endNum = totalSize;
            }
            paramMap.put("Count", endNum - startNum);
            paramMap.put("List", list.subList(startNum, endNum));
            HttpEntity entity = core.getMyHttpClient().doPost(url, JSON.toJSONString(paramMap), getPersistentCookieMap());
            try {
                String text = EntityUtils.toString(entity, Consts.UTF_8);
                JSONObject obj = JSON.parseObject(text);
                JSONArray contactList = obj.getJSONArray("ContactList");
                int contactSize = contactList.size();
                if (contactSize > 0) {
                    for (int j = 0; j < contactSize; j++) {
                        String userName = contactList.getJSONObject(j).getString("UserName");
                        if (userName.startsWith("@@")) {
                            CoreManage.addNewGroup(core, contactList.getJSONObject(j));
                        } else if (userName.startsWith("@")) {
                            CoreManage.addNewContact(core, contactList.getJSONObject(j));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.info(e.getMessage());
            }
        }
    }


    /**
     * 检查登陆状态
     *
     * @param result
     * @return
     */
    public String checklogin(String result) {
        String regEx = "window.code=(\\d+)";
        Matcher matcher = CommonTools.getMatcher(regEx, result);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 处理登陆信息
     *
     * @param loginContent
     * @author https://github.com/yaphone
     * @date 2017年4月9日 下午12:16:26
     */
    private boolean processLoginInfo(String loginContent) {
        String regEx = "window.redirect_uri=\"(\\S+)\";";
        Matcher matcher = CommonTools.getMatcher(regEx, loginContent);
        if (matcher.find()) {
            String originalUrl = matcher.group(1);
            String url = originalUrl.substring(0, originalUrl.lastIndexOf('/'));
            core.getLoginInfo().put("url", url);
            Map<String, List<String>> possibleUrlMap = this.getPossibleUrlMap();
            Iterator<Entry<String, List<String>>> iterator = possibleUrlMap.entrySet().iterator();
            Entry<String, List<String>> entry;
            String fileUrl;
            String syncUrl;
            while (iterator.hasNext()) {
                entry = iterator.next();
                String indexUrl = entry.getKey();
                fileUrl = "https://" + entry.getValue().get(0) + "/cgi-bin/mmwebwx-bin";
                syncUrl = "https://" + entry.getValue().get(1) + "/cgi-bin/mmwebwx-bin";
                if (core.getLoginInfo().get("url").toString().contains(indexUrl)) {
                    core.setIndexUrl(indexUrl);
                    core.getLoginInfo().put("fileUrl", fileUrl);
                    core.getLoginInfo().put("syncUrl", syncUrl);
                    break;
                }
            }
            if (core.getLoginInfo().get("fileUrl") == null && core.getLoginInfo().get("syncUrl") == null) {
                core.getLoginInfo().put("fileUrl", url);
                core.getLoginInfo().put("syncUrl", url);
            }
            core.getLoginInfo().put("deviceid", "e" + String.valueOf(new Random().nextLong()).substring(1, 16)); // 生成15位随机数
            core.getLoginInfo().put("BaseRequest", new ArrayList<String>());
            String text = "";

            try {
                Map<String, String> header = new HashMap<>();
                header.put("client-version", "2.0.0");
                header.put("extspam", "Go8FCIkFEokFCggwMDAwMDAwMRAGGvAESySibk50w5Wb3uTl2c2h64jVVrV7gNs06GFlWplHQbY/5FfiO++1yH4ykCyNPWKXmco+wfQzK5R98D3so7rJ5LmGFvBLjGceleySrc3SOf2Pc1gVehzJgODeS0lDL3/I/0S2SSE98YgKleq6Uqx6ndTy9yaL9qFxJL7eiA/R3SEfTaW1SBoSITIu+EEkXff+Pv8NHOk7N57rcGk1w0ZzRrQDkXTOXFN2iHYIzAAZPIOY45Lsh+A4slpgnDiaOvRtlQYCt97nmPLuTipOJ8Qc5pM7ZsOsAPPrCQL7nK0I7aPrFDF0q4ziUUKettzW8MrAaiVfmbD1/VkmLNVqqZVvBCtRblXb5FHmtS8FxnqCzYP4WFvz3T0TcrOqwLX1M/DQvcHaGGw0B0y4bZMs7lVScGBFxMj3vbFi2SRKbKhaitxHfYHAOAa0X7/MSS0RNAjdwoyGHeOepXOKY+h3iHeqCvgOH6LOifdHf/1aaZNwSkGotYnYScW8Yx63LnSwba7+hESrtPa/huRmB9KWvMCKbDThL/nne14hnL277EDCSocPu3rOSYjuB9gKSOdVmWsj9Dxb/iZIe+S6AiG29Esm+/eUacSba0k8wn5HhHg9d4tIcixrxveflc8vi2/wNQGVFNsGO6tB5WF0xf/plngOvQ1/ivGV/C1Qpdhzznh0ExAVJ6dwzNg7qIEBaw+BzTJTUuRcPk92Sn6QDn2Pu3mpONaEumacjW4w6ipPnPw+g2TfywJjeEcpSZaP4Q3YV5HG8D6UjWA4GSkBKculWpdCMadx0usMomsSS/74QgpYqcPkmamB4nVv1JxczYITIqItIKjD35IGKAUwAA==");
                originalUrl = originalUrl + "&fun=new&version=v2&mod=desktop&lang=zh_CN";
                HttpEntity entity = core.getMyHttpClient().doGet(originalUrl, null, false, header);
                text = EntityUtils.toString(entity);
            } catch (Exception e) {
                LOG.info(e.getMessage());
                return false;
            }
            //add by 默非默 2017-08-01 22:28:09
            //如果登录被禁止时，则登录返回的message内容不为空，下面代码则判断登录内容是否为空，不为空则退出程序
            String msg = getLoginMessage(text);
            if (!"".equals(msg)) {
                LOG.info(msg);
//				System.exit(0);
                return false;
            }
            Document doc = CommonTools.xmlParser(text);
            if (doc != null) {
                core.getLoginInfo().put(StorageLoginInfoEnum.skey.getKey(),
                    doc.getElementsByTagName(StorageLoginInfoEnum.skey.getKey()).item(0).getFirstChild()
                        .getNodeValue());
                core.getLoginInfo().put(StorageLoginInfoEnum.wxsid.getKey(),
                    doc.getElementsByTagName(StorageLoginInfoEnum.wxsid.getKey()).item(0).getFirstChild()
                        .getNodeValue());
                core.getLoginInfo().put(StorageLoginInfoEnum.wxuin.getKey(),
                    doc.getElementsByTagName(StorageLoginInfoEnum.wxuin.getKey()).item(0).getFirstChild()
                        .getNodeValue());
                core.getLoginInfo().put(StorageLoginInfoEnum.pass_ticket.getKey(),
                    doc.getElementsByTagName(StorageLoginInfoEnum.pass_ticket.getKey()).item(0).getFirstChild()
                        .getNodeValue());
            }

        }
        return true;
    }

    private Map<String, List<String>> getPossibleUrlMap() {
        Map<String, List<String>> possibleUrlMap = new HashMap<String, List<String>>();
        possibleUrlMap.put("wx.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx.qq.com");
                add("webpush.wx.qq.com");
            }
        });

        possibleUrlMap.put("wx2.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx2.qq.com");
                add("webpush.wx2.qq.com");
            }
        });
        possibleUrlMap.put("wx8.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx8.qq.com");
                add("webpush.wx8.qq.com");
            }
        });

        possibleUrlMap.put("web2.wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web2.wechat.com");
                add("webpush.web2.wechat.com");
            }
        });
        possibleUrlMap.put("wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web.wechat.com");
                add("webpush.web.wechat.com");
            }
        });
        return possibleUrlMap;
    }

    /**
     * 同步消息 sync the messages
     *
     * @return
     * @author https://github.com/yaphone
     * @date 2017年5月12日 上午12:24:55
     */
    private JSONObject webWxSync() {
        JSONObject result = null;
        String url = String.format(URLEnum.WEB_WX_SYNC_URL.getUrl(),
            core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()),
            core.getLoginInfo().get(StorageLoginInfoEnum.wxsid.getKey()),
            core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey()),
            core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = core.getParamMap();
        paramMap.put(StorageLoginInfoEnum.SyncKey.getKey(),
            core.getLoginInfo().get(StorageLoginInfoEnum.SyncKey.getKey()));
        paramMap.put("rr", -System.currentTimeMillis() / 1000);
        String paramStr = JSON.toJSONString(paramMap);
        try {
            HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr, getPersistentCookieMap());
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            if (obj.getJSONObject("BaseResponse").getInteger("Ret") != 0) {
                result = null;
            } else {
                result = obj;
                core.getLoginInfo().put(StorageLoginInfoEnum.SyncKey.getKey(), obj.getJSONObject("SyncCheckKey"));
                JSONArray syncArray = obj.getJSONObject(StorageLoginInfoEnum.SyncKey.getKey()).getJSONArray("List");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < syncArray.size(); i++) {
                    sb.append(syncArray.getJSONObject(i).getString("Key") + "_"
                        + syncArray.getJSONObject(i).getString("Val") + "|");
                }
                String synckey = sb.toString();
                /**
                 * 随着每次获取最新消息（参见9）后的返回值更新，其目的在于每次同步消息后记录一个当前同步的状态
                 */
                core.getLoginInfo().put(StorageLoginInfoEnum.synckey.getKey(),
                    synckey.substring(0, synckey.length() - 1));// 1_656161336|2_656161626|3_656161313|11_656159955|13_656120033|201_1492273724|1000_1492265953|1001_1492250432|1004_1491805192
            }
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
        return result;

    }

    /**
     * 检查是否有新消息 check whether there's a message
     *
     * @return
     * @author https://github.com/yaphone
     * @date 2017年4月16日 上午11:11:34
     */
    private Map<String, String> syncCheck() {
        Map<String, String> resultMap = new HashMap<String, String>();
        // 组装请求URL和参数
        String url = core.getLoginInfo().get(StorageLoginInfoEnum.syncUrl.getKey()) + URLEnum.SYNC_CHECK_URL.getUrl();
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        for (BaseParaEnum baseRequest : BaseParaEnum.values()) {
            params.add(new BasicNameValuePair(baseRequest.para().toLowerCase(),
                core.getLoginInfo().get(baseRequest.value()).toString()));
        }
        params.add(new BasicNameValuePair("r", String.valueOf(System.currentTimeMillis())));
        params.add(new BasicNameValuePair("synckey", (String) core.getLoginInfo().get("synckey")));
        params.add(new BasicNameValuePair("_", String.valueOf(System.currentTimeMillis())));
        try {
            HttpEntity entity = core.getMyHttpClient().doGet(url, params, true, getPersistentCookieMap());
            if (entity == null) {
                resultMap.put("retcode", "9999");
                resultMap.put("selector", "9999");
                return resultMap;
            }
            String text = EntityUtils.toString(entity);
            String regEx = "window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}";
            Matcher matcher = CommonTools.getMatcher(regEx, text);
            if (!matcher.find() || matcher.group(1).equals("2")) {
                LOG.info(String.format("Unexpected sync check result: %s", text));
            } else {
                resultMap.put("retcode", matcher.group(1));
                resultMap.put("selector", matcher.group(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    private Map<String, String> getPersistentCookieMap() {
        /**
         * 以下部分确保cookie存在header中，重启时需要强制手动添加，
         * TODO 不强制加重启会cookie失效,还不确定问题
         */
        String cookieStr = null;
        if (core != null && core.getMyHttpClient() != null && core.getMyHttpClient().getCookieStore() != null) {
            List<Cookie> cookies = core.getMyHttpClient().getCookieStore().getCookies();
            StringBuilder sb = new StringBuilder();
            if (CollectionUtil.isNotEmpty(cookies)) {
                for (Cookie ck : cookies) {
                    sb.append(ck.getName()).append('=').append(ck.getValue()).append(';');
                }
                cookieStr = sb.toString().substring(0, sb.toString().length() - 1);
            }

        }
        Map<String, String> headerMap = null;
        if (cookieStr != null) {
            headerMap = new HashMap<>(6);
            headerMap.put("Cookie", cookieStr);
        }
        /* ^-----------------------------------------------^ */
        return headerMap;
    }

    /**
     * 解析登录返回的消息，如果成功登录，则message为空
     *
     * @param result
     * @return
     */
    public String getLoginMessage(String result) {
        String[] strArr = result.split("<message>");
        String[] rs = strArr[1].split("</message>");
        if (rs != null && rs.length > 1) {
            return rs[0];
        }
        return "";
    }
}
