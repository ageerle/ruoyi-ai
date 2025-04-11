package org.ruoyi.common.wechat.itchat4j.core;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.ruoyi.common.wechat.itchat4j.beans.BaseMsg;
import org.ruoyi.common.wechat.itchat4j.beans.SendMsg;
import org.ruoyi.common.wechat.itchat4j.beans.User;
import org.ruoyi.common.wechat.itchat4j.client.HttpClientManage;
import org.ruoyi.common.wechat.itchat4j.client.SingleHttpClient;
import org.ruoyi.common.wechat.itchat4j.service.impl.LoginServiceImpl;
import org.ruoyi.common.wechat.itchat4j.utils.enums.parameters.BaseParaEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心存储类，全局只保存一份，单例模式
 *
 * @author https://github.com/yaphone
 * @author WesleyOne 修改
 * @version 1.0
 * @date 创建时间：2017年4月23日 下午2:33:56
 */
public class Core {

    private Core() {

    }

    private Core(String uniqueKey) {
        this.uniqueKey = uniqueKey;
        this.myHttpClient = HttpClientManage.getInstance(this.uniqueKey);
        this.threadGroup = new ThreadGroup(this.uniqueKey);
    }

    protected static Core getInstance(String uniqueKey) {
        return new Core(uniqueKey);
    }

    private String uniqueKey;

    boolean alive = false;

    /**
     * 管理当前机器人的所有业务线程
     */
    @JSONField(serialize = false)
    private ThreadGroup threadGroup;

    @JSONField(serialize = false)
    boolean isFinishInit = false;

    /**
     * login,webWxInit
     * 登录接口获取
     */
    private String indexUrl;
    private String userName;
    private String nickName;
    String uuid = null;
    /**
     * webWxInit
     * InviteStartCount
     * SyncKey
     * synckey 随着每次获取最新消息后的返回值更新，其目的在于每次同步消息后记录一个当前同步的状态
     */
    Map<String, Object> loginInfo = new HashMap<String, Object>();

    /**
     * webWxInit
     * 登陆账号自身信息
     *
     * @see User
     */
    private JSONObject userSelf;


    /**
     * 初始化/cgi-bin/mmwebwx-bin/webwxinit
     * 最后一次收到正常retcode的时间，秒为单位
     * <p>
     * synccheck刷新
     */
    private long lastNormalRetcodeTime;


    /**
     * synccheck和webWxSync容错次数,超过退出
     */
    int receivingRetryCount = 5;


    @JSONField(serialize = false)
    SingleHttpClient myHttpClient;

    public SingleHttpClient getMyHttpClient() {
        return HttpClientManage.getInstance(uniqueKey);
    }

    /**
     * 初始话时获取联系人时创建
     * @see LoginServiceImpl#webWxGetContact()
     */
    /**
     * memberList长度
     */
    @JSONField(serialize = false)
    private int memberCount = 0;
    /**
     * 好友+群聊+公众号+特殊账号
     * 注意：不主动插入,获取时通过其他几个账号集合合并
     */
    @JSONField(serialize = false)
    private List<JSONObject> memberList = new ArrayList<JSONObject>();
    /**
     * 好友
     */
    @JSONField(serialize = false)
    private List<JSONObject> contactList = new ArrayList<JSONObject>();
    /**
     * 群
     */
    @JSONField(serialize = false)
    private List<JSONObject> groupList = new ArrayList<JSONObject>();
    /**
     * 公众号/服务号
     */
    @Deprecated
    @JSONField(serialize = false)
    private List<JSONObject> publicUsersList = new ArrayList<JSONObject>();
    /**
     * 特殊账号
     */
    @JSONField(serialize = false)
    private List<JSONObject> specialUsersList = new ArrayList<JSONObject>();

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */

    /**
     * synccheck和webWxSynct添加
     * 异步接受消息存储
     */
    @JSONField(serialize = false)
    private List<BaseMsg> msgList = new ArrayList<>();

    /**
     * 异步发送消息存储
     */
    @JSONField(serialize = false)
    private List<SendMsg> sendList = new ArrayList();

    /********************************
     * 缓存字段,用于快速查找
     ********************************/

    /**
     * 微信昵称不能超过16位，而ID比较长，干脆用一个Map
     * 群ID或昵称,群信息
     * 注意:存在相同昵称会后者覆盖前者
     * <p>
     * WebWxBatchGetContact之后可以通过
     * .getJSONArray("MemberList")获取群成员列表
     */
    @JSONField(serialize = false)
    private Map<String, JSONObject> groupInfoMap = new HashMap<>(1024);

    /**
     * 微信昵称不能超过16位，而ID比较长，干脆用一个Map
     * 玩家ID或昵称，玩家信息
     * 注意:存在相同昵称会后者覆盖前者
     */
    @JSONField(serialize = false)
    private Map<String, JSONObject> userInfoMap = new HashMap<>(1024);

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */

    /**
     * 请求参数
     */
    @JSONField(serialize = false)
    public Map<String, Object> getParamMap() {
        return new HashMap<String, Object>(1) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                Map<String, String> map = new HashMap<>(16);
                for (BaseParaEnum baseRequest : BaseParaEnum.values()) {
                    map.put(baseRequest.para(), getLoginInfo().get(baseRequest.value()).toString());
                }
                put("BaseRequest", map);
            }
        };
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive) {
            this.isFinishInit = false;
        }
    }

    public String getIndexUrl() {
        return indexUrl;
    }

    public void setIndexUrl(String indexUrl) {
        this.indexUrl = indexUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, Object> getLoginInfo() {
        return loginInfo;
    }

    public void setLoginInfo(Map<String, Object> loginInfo) {
        this.loginInfo = loginInfo;
    }

    public JSONObject getUserSelf() {
        return userSelf;
    }

    public void setUserSelf(JSONObject userSelf) {
        this.userSelf = userSelf;
    }

    public long getLastNormalRetcodeTime() {
        return lastNormalRetcodeTime;
    }

    public void setLastNormalRetcodeTime(long lastNormalRetcodeTime) {
        this.lastNormalRetcodeTime = lastNormalRetcodeTime;
    }

    public int getReceivingRetryCount() {
        return receivingRetryCount;
    }

    public void setReceivingRetryCount(int receivingRetryCount) {
        this.receivingRetryCount = receivingRetryCount;
    }

    public int getMemberCount() {
        return getContactList().size() + getGroupList().size() + getPublicUsersList().size() + getSpecialUsersList().size();
    }

    public List<JSONObject> getMemberList() {
        List<JSONObject> memberList = new ArrayList<>();
        memberList.addAll(this.getContactList());
        memberList.addAll(this.getGroupList());
        memberList.addAll(this.getPublicUsersList());
        memberList.addAll(this.getSpecialUsersList());
        return memberList;
    }

    public List<JSONObject> getContactList() {
        return contactList;
    }

    public void setContactList(List<JSONObject> contactList) {
        this.contactList = contactList;
    }

    public List<JSONObject> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<JSONObject> groupList) {
        this.groupList = groupList;
    }

    public List<JSONObject> getPublicUsersList() {
        return publicUsersList;
    }

    public void setPublicUsersList(List<JSONObject> publicUsersList) {
        this.publicUsersList = publicUsersList;
    }

    public List<JSONObject> getSpecialUsersList() {
        return specialUsersList;
    }

    public void setSpecialUsersList(List<JSONObject> specialUsersList) {
        this.specialUsersList = specialUsersList;
    }

    public List<BaseMsg> getMsgList() {
        return msgList;
    }

    public void setMsgList(List<BaseMsg> msgList) {
        this.msgList = msgList;
    }

    public Map<String, JSONObject> getGroupInfoMap() {
        return groupInfoMap;
    }

    public void setGroupInfoMap(Map<String, JSONObject> groupInfoMap) {
        this.groupInfoMap = groupInfoMap;
    }

    public Map<String, JSONObject> getUserInfoMap() {
        return userInfoMap;
    }

    public void setUserInfoMap(Map<String, JSONObject> userInfoMap) {
        this.userInfoMap = userInfoMap;
    }

    public boolean isFinishInit() {
        return isFinishInit;
    }

    public void setFinishInit(boolean finishInit) {
        isFinishInit = finishInit;
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public void setThreadGroup(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    public List<SendMsg> getSendList() {
        return sendList;
    }
}
