package tech.ordinaryroad.live.chat.client.douyu.util;

import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.IDouyuMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author mjz
 * @date 2023/8/27
 */
class DouyuCodecUtilTest {

    @Test
    void test() {
        String loginres = "type@=loginres/userid@=1235403446/roomgroup@=0/pg@=0/sessionid@=0/username@=/nickname@=/live_stat@=0/is_illegal@=0/ill_ct@=/ill_ts@=0/now@=0/ps@=0/es@=0/it@=0/its@=0/npv@=0/best_dlev@=0/cur_lev@=0/nrc@=2785047409/ih@=0/sid@=72256/sahf@=0/sceneid@=0/newrg@=0/regts@=0/ip@=112.43.93.229/rn@=0/rct@=0/";
        String pingreq = "type@=pingreq/tick@=1693128084471/";
        String mrkl = "type@=mrkl/";
        String subres = "type@=subres/res@=0/";
        String mapkb = "type@=mapkb/cmd@=3/rid@=5515841/pk_time@=600/lt@=156/teams@=team@AA=5@ASres@AA=1@ASsc@AA=660000@ASbf@AA=0@AS@Steam@AA=6@ASres@AA=2@ASsc@AA=600000@ASbf@AA=0@AS@Steam@AA=4@ASres@AA=3@ASsc@AA=456000@ASbf@AA=0@AS@Steam@AA=2@ASres@AA=4@ASsc@AA=302000@ASbf@AA=0@AS@Steam@AA=3@ASres@AA=5@ASsc@AA=100000@ASbf@AA=0@AS@Steam@AA=1@ASres@AA=6@ASsc@AA=200@ASbf@AA=0@AS@S/members@=rid@AA=5515841@ASuid@AA=219585944@ASnick@AA=喵小莎@ASicon@AA=avatar_v3@AAS202303@AAS4cab38d935004acfab53f77a730c9d42@ASstatus@AA=0@ASsc@AA=100000@AStime@AA=1693127363@ASteam@AA=3@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=8733064@ASuid@AA=364378905@ASnick@AA=阿允唱情歌@ASicon@AA=avatar_v3@AAS202008@AAS31b239d8af174462b5e0a2990c70b818@ASstatus@AA=0@ASsc@AA=600000@AStime@AA=1693127527@ASteam@AA=6@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=10673032@ASuid@AA=442218984@ASnick@AA=果酱Broly@ASicon@AA=avatar_v3@AAS202306@AASf7d61a0ea3bb4c179af7b5e24f94953e@ASstatus@AA=0@ASsc@AA=200@AStime@AA=1693127347@ASteam@AA=1@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=533813@ASuid@AA=36922190@ASnick@AA=正直博@ASicon@AA=avatar_v3@AAS202212@AAS4e340983996f43b991ffa50af7b956f6@ASstatus@AA=0@ASsc@AA=302000@AStime@AA=1693127348@ASteam@AA=2@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=8031896@ASuid@AA=243111494@ASnick@AA=青榎@ASicon@AA=avatar_v3@AAS202305@AAS8bd800d3bb8d4e16884dcb3e3f77b038@ASstatus@AA=0@ASsc@AA=456000@AStime@AA=1693127393@ASteam@AA=4@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=7009686@ASuid@AA=299482114@ASnick@AA=紫薇同学@ASicon@AA=avatar_v3@AAS202302@AASb67044b78b494b0896fd6738bc2d5b7b@ASstatus@AA=0@ASsc@AA=660000@AStime@AA=1693127424@ASteam@AA=5@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@S/subcmd@=0/pkm@=0/rlt@=0/";
        String pdrinfo = "giftId@=23518/total@=total@A=3@Snum@A=0@Sfin@A=0@S/actStatus@=1/time@=1693158017596/type@=pdrinfo/dayFin@=0/list@=1@A=total@AAA=6@AASnum@AAA=6@AASpid@AAA=2682@AASfin@AAA=1@AAS@AStotal@AAA=600@AASnum@AAA=336@AASpid@AAA=2680@AASfin@AAA=0@AAS@AS@S100@A=total@AAA=2@AASnum@AAA=0@AASpid@AAA=2685@AASfin@AAA=0@AAS@AStotal@AAA=100@AASnum@AAA=5@AASpid@AAA=2682@AASfin@AAA=0@AAS@AS@S10@A=total@AAA=2@AASnum@AAA=0@AASpid@AAA=2683@AASfin@AAA=0@AAS@AStotal@AAA=80@AASnum@AAA=35@AASpid@AAA=2681@AASfin@AAA=0@AAS@AS@S/";

        Map<String, String> map = new HashMap<>();
        map.put("loginres", loginres);
        map.put("pingreq", pingreq);
        map.put("mrkl", mrkl);
        map.put("subres", subres);
        map.put("mapkb", mapkb);
        map.put("pdrinfo", pdrinfo);

        map.forEach((string, string2) -> {
            BaseDouyuCmdMsg baseDouyuCmdMsg = (BaseDouyuCmdMsg) DouyuCodecUtil.parseDouyuSttString(string2, DouyuCodecUtil.MSG_TYPE_RECEIVE);
            assertNotNull(baseDouyuCmdMsg);
            String cmd = baseDouyuCmdMsg.getCmd();
            assertEquals(string, cmd);
        });
    }

    @Test
    void decodeTest() {
        String mapkb = "type@=mapkb/cmd@=3/rid@=5515841/pk_time@=600/lt@=156/teams@=team@AA=5@ASres@AA=1@ASsc@AA=660000@ASbf@AA=0@AS@Steam@AA=6@ASres@AA=2@ASsc@AA=600000@ASbf@AA=0@AS@Steam@AA=4@ASres@AA=3@ASsc@AA=456000@ASbf@AA=0@AS@Steam@AA=2@ASres@AA=4@ASsc@AA=302000@ASbf@AA=0@AS@Steam@AA=3@ASres@AA=5@ASsc@AA=100000@ASbf@AA=0@AS@Steam@AA=1@ASres@AA=6@ASsc@AA=200@ASbf@AA=0@AS@S/members@=rid@AA=5515841@ASuid@AA=219585944@ASnick@AA=喵小莎@ASicon@AA=avatar_v3@AAS202303@AAS4cab38d935004acfab53f77a730c9d42@ASstatus@AA=0@ASsc@AA=100000@AStime@AA=1693127363@ASteam@AA=3@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=8733064@ASuid@AA=364378905@ASnick@AA=阿允唱情歌@ASicon@AA=avatar_v3@AAS202008@AAS31b239d8af174462b5e0a2990c70b818@ASstatus@AA=0@ASsc@AA=600000@AStime@AA=1693127527@ASteam@AA=6@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=10673032@ASuid@AA=442218984@ASnick@AA=果酱Broly@ASicon@AA=avatar_v3@AAS202306@AASf7d61a0ea3bb4c179af7b5e24f94953e@ASstatus@AA=0@ASsc@AA=200@AStime@AA=1693127347@ASteam@AA=1@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=533813@ASuid@AA=36922190@ASnick@AA=正直博@ASicon@AA=avatar_v3@AAS202212@AAS4e340983996f43b991ffa50af7b956f6@ASstatus@AA=0@ASsc@AA=302000@AStime@AA=1693127348@ASteam@AA=2@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=8031896@ASuid@AA=243111494@ASnick@AA=青榎@ASicon@AA=avatar_v3@AAS202305@AAS8bd800d3bb8d4e16884dcb3e3f77b038@ASstatus@AA=0@ASsc@AA=456000@AStime@AA=1693127393@ASteam@AA=4@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@Srid@AA=7009686@ASuid@AA=299482114@ASnick@AA=紫薇同学@ASicon@AA=avatar_v3@AAS202302@AASb67044b78b494b0896fd6738bc2d5b7b@ASstatus@AA=0@ASsc@AA=660000@AStime@AA=1693127424@ASteam@AA=5@ASgroup_no@AA=0@ASmd@AA=0@AShvsc@AA=0@ASct@AA=0@AS@S/subcmd@=0/pkm@=0/rlt@=0/";
        Map<String, Object> stringObjectMap = DouyuCodecUtil.parseDouyuSttStringToMap(mapkb);
        assertNotNull(stringObjectMap);
        stringObjectMap.forEach((string, o) -> {
            System.out.println(string);
        });
    }

    @Test
    void decodeTest2() {
        String pdrinfo = "giftId@=23518/total@=total@A=3@Snum@A=0@Sfin@A=0@S/actStatus@=1/time@=1693158017596/type@=pdrinfo/dayFin@=0/list@=1@A=total@AAA=6@AASnum@AAA=6@AASpid@AAA=2682@AASfin@AAA=1@AAS@AStotal@AAA=600@AASnum@AAA=336@AASpid@AAA=2680@AASfin@AAA=0@AAS@AS@S100@A=total@AAA=2@AASnum@AAA=0@AASpid@AAA=2685@AASfin@AAA=0@AAS@AStotal@AAA=100@AASnum@AAA=5@AASpid@AAA=2682@AASfin@AAA=0@AAS@AS@S10@A=total@AAA=2@AASnum@AAA=0@AASpid@AAA=2683@AASfin@AAA=0@AAS@AStotal@AAA=80@AASnum@AAA=35@AASpid@AAA=2681@AASfin@AAA=0@AAS@AS@S/";
        Map<String, Object> stringObjectMap = DouyuCodecUtil.parseDouyuSttStringToMap(pdrinfo);
        assertNotNull(stringObjectMap);
        stringObjectMap.forEach((string, o) -> {
            System.out.println(string);
        });
    }

    @Test
    void toDouyuSttStringTest() {
        String douyuSttString = DouyuCodecUtil.toDouyuSttString(new HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", 2);
            put("key3", new ArrayList<Map<String, Object>>() {{
                add(new HashMap<String, Object>() {{
                    put("11", 11);
                    put("12", 12);
                }});
                add(new HashMap<String, Object>() {{
                    put("21", 21);
                    put("22", 22);
                }});
            }});
        }});
        System.out.println(douyuSttString);
        assertEquals("key1@=value1/key2@=2/key3@=11@AA=11@AS12@AA=12@AS@S22@AA=22@AS21@AA=21@AS@S/".length(), douyuSttString.length());
    }

    @Test
    void codecTest() {
        String mapkb = "type@=mapkb/pk_time@=600/teams@=team@AA=5@ASres@AA=1@ASsc@AA=660000@ASbf@AA=0@AS@Steam@AA=6@ASres@AA=2@ASsc@AA=600000@ASbf@AA=0@AS@Steam@AA=4@ASres@AA=3@ASsc@AA=456000@ASbf@AA=0@AS@Steam@AA=2@ASres@AA=4@ASsc@AA=302000@ASbf@AA=0@AS@Steam@AA=3@ASres@AA=5@ASsc@AA=100000@ASbf@AA=0@AS@Steam@AA=1@ASres@AA=6@ASsc@AA=200@ASbf@AA=0@AS@S/";
        IDouyuMsg iDouyuMsg = DouyuCodecUtil.parseDouyuSttString(mapkb, DouyuCodecUtil.MSG_TYPE_RECEIVE);
        String douyuSttString = DouyuCodecUtil.toDouyuSttString(iDouyuMsg);
        System.out.println(mapkb);
        System.out.println(douyuSttString);
        assertEquals(mapkb.length(), douyuSttString.length());
        IDouyuMsg douyuSttStringMsg = DouyuCodecUtil.parseDouyuSttString(douyuSttString, DouyuCodecUtil.MSG_TYPE_RECEIVE);
        assertNotNull(douyuSttStringMsg);
    }

    @Test
    void unescape() {
        String unescape = DouyuCodecUtil.unescape("team@AA=5@ASres@AA=1@ASsc@AA=660000@ASbf@AA=0@AS@Steam@AA=6@ASres@AA=2@ASsc@AA=600000@ASbf@AA=0@AS@Steam@AA=4@ASres@AA=3@ASsc@AA=456000@ASbf@AA=0@AS@Steam@AA=2@ASres@AA=4@ASsc@AA=302000@ASbf@AA=0@AS@Steam@AA=3@ASres@AA=5@ASsc@AA=100000@ASbf@AA=0@AS@Steam@AA=1@ASres@AA=6@ASsc@AA=200@ASbf@AA=0@AS@S");
        System.out.println(unescape);
    }
}