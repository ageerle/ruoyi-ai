package tech.ordinaryroad.live.chat.client.huya.util;

import cn.hutool.core.codec.Base64;
import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.huya.msg.ConnectParaInfo;
import tech.ordinaryroad.live.chat.client.huya.msg.LiveLaunchRsp;
import tech.ordinaryroad.live.chat.client.huya.msg.WebSocketCommand;
import tech.ordinaryroad.live.chat.client.huya.msg.WupRsp;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.UserId;
import tech.ordinaryroad.live.chat.client.huya.msg.req.*;

import java.util.List;

/**
 * @author mjz
 * @date 2023/10/3
 */
class HuyaCodecUtilTest {

    private String ver = "2309271152";
    private String exp = "15547.23738,16582.25335,32083.50834";
    private String appSrc = "HUYA&ZH&2052";

    @Test
    void ab2str() {
        ConnectParaInfo wsConnectParaInfo = ConnectParaInfo.newWSConnectParaInfo(ver, exp, appSrc);
        byte[] byteArray = wsConnectParaInfo.toByteArray();
        String s = HuyaCodecUtil.ab2str(byteArray);
    }

    @Test
    void btoa() {
        ConnectParaInfo wsConnectParaInfo = ConnectParaInfo.newWSConnectParaInfo(ver, exp, appSrc);
        byte[] byteArray = wsConnectParaInfo.toByteArray();
        String s = HuyaCodecUtil.ab2str(byteArray);

        String btoa = HuyaCodecUtil.btoa(s);
    }

    @Test
    void decodeHeartbeatTest() {
        byte[] decode = Base64.decode("AAMdAAEEKAAABCgQAyw8QAFWCG9ubGluZXVpZg9PblVzZXJIZWFydEJlYXR9AAED+QgAAQYEdFJlcR0AAQPrCgoMFiAwYTdkY2E3MmEzY2UxYjY1NDAwMWRkMmFkZTJhZTg1NyYANhp3ZWJoNSYyMzA5MjcxMTUyJndlYnNvY2tldEcAAAOKdnBsYXllcl9zYmFubmVyXzE3MjQ2OTFfMTcyNDY5MT0xOyBTb3VuZFZhbHVlPTAuNTA7IGFscGhhVmFsdWU9MC44MDsgZ2FtZV9kaWQ9UjI0SjJnMG1CenZkWEpmN2E5bmhlU2wzekljaTJCT3AwLXQ7IGlzSW5MaXZlUm9vbT10cnVlOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyBfX3lhbWlkX3R0MT0wLjgwMjk5MzUwNjUwMTEyNjk7IF9feWFtaWRfbmV3PUNBNzVENENENUMxMDAwMDExNjVCODJCNTIxNDBDOTAwOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyB1ZGJfZ3VpZGRhdGE9YWY1ZGJkYmY3NjI1NGE2ZThhMGEyOTNjY2FlOWI2ODg7IHVkYl9kZXZpY2VpZD13Xzc2MTYyMzQ4Mzg2MTgxNTI5NjsgdWRiX3Bhc3NkYXRhPTM7IF9feWFzbWlkPTAuODAyOTkzNTA2NTAxMTI2OTsgSG1fbHZ0XzUxNzAwYjZjNzIyZjViYjRjZjM5OTA2YTU5NmVhNDFmPTE2OTY0MDk0MjAsMTY5NjQxMTgwNiwxNjk2NDczNzYwLDE2OTY0NzY3NDU7IF95YXNpZHM9X19yb290c2lkJTNEQ0E3NjY5MjcwM0IwMDAwMTNGRkJCRkVBNjY4Nzk5RDA7IEhtX2xwdnRfNTE3MDBiNmM3MjJmNWJiNGNmMzk5MDZhNTk2ZWE0MWY9MTY5NjQ3Njg4MDsgaHV5YV91YT13ZWJoNSYwLjAuMSZhY3Rpdml0eTsgX3JlcF9jbnQ9Mzsgc2RpZD0wVW5IVWd2MC9xbWZENEtBS2x3emhxU1drU1d3a3RzZXhQSWNsTnQzUGJ3TVFha1dOTmZrMlc4KzFkNlFNZzVZYnU1bC9GcDRpaHRUTDdCd0Q2bTQ0MENHUUNkU0htZW1kOUNNLzVKRVZkanJXVmtuOUx0ZkZKdy9RbzRrZ0tyOE9aSERxTm51d2c2MTJzR3lmbEZuMWRrVWVaWVRUb0N6emw0R0NIcTdNVURhaHhHdVBSOG1VZGRmSW1GdGpjY3MxOyBodXlhX2ZsYXNoX3JlcF9jbnQ9NTc7IHJlcF9jbnQ9NDA7IGh1eWFfd2ViX3JlcF9jbnQ9MTY2XGYGY2hyb21lCxwsQgAaURNcYP98jJysC4yYDKgMLDYlYTgzMDdmMWM0YzRmNGVmMDphODMwN2YxYzRjNGY0ZWYwOjA6MExcZiAyYmZlZjAzNmEwMzBkOTgyN2ZjYmQwMmU5ZmM0NzY1OQ==");
        WupReq wupReq = new WupReq();
        wupReq.decode(decode);
        UserHeartBeatReq userHeartBeatReq = new UserHeartBeatReq();
        userHeartBeatReq = wupReq.getUniAttribute().getByClass("tReq", userHeartBeatReq);
        long lPid = userHeartBeatReq.getLPid();
    }

    @Test
    void decodeRegisterGroupReq() {
        byte[] decode = Base64.decode("ABAdAAAhCQACBgxsaXZlOjE3MjQ2OTEGDGNoYXQ6MTcyNDY5MRYAIAE2AExcZgA=");
        WebSocketCommand webSocketCommand = new WebSocketCommand(HuyaCodecUtil.newUtf8TarsInputStream(decode));
        byte[] vData = webSocketCommand.getVData();
        RegisterGroupReq registerGroupReq = new RegisterGroupReq();
        registerGroupReq.readFrom(HuyaCodecUtil.newUtf8TarsInputStream(vData));
        List<String> vGroupId = registerGroupReq.getVGroupId();
    }

    @Test
    void decodeLiveLaunchReq() {
        byte[] decode = Base64.decode("AAMdAAEEJAAABCQQAyw8QARWBmxpdmV1aWYIZG9MYXVuY2h9AAED/ggAAQYEdFJlcR0AAQPwCgoMFiAwYTdkY2E3MmEzY2UxYjY1NDAwMWRkMmFkZTJhZTg1NyYANhp3ZWJoNSYyMzA5MjcxMTUyJndlYnNvY2tldEcAAAOKdnBsYXllcl9zYmFubmVyXzE3MjQ2OTFfMTcyNDY5MT0xOyBTb3VuZFZhbHVlPTAuNTA7IGFscGhhVmFsdWU9MC44MDsgZ2FtZV9kaWQ9UjI0SjJnMG1CenZkWEpmN2E5bmhlU2wzekljaTJCT3AwLXQ7IGlzSW5MaXZlUm9vbT10cnVlOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyBfX3lhbWlkX3R0MT0wLjgwMjk5MzUwNjUwMTEyNjk7IF9feWFtaWRfbmV3PUNBNzVENENENUMxMDAwMDExNjVCODJCNTIxNDBDOTAwOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyB1ZGJfZ3VpZGRhdGE9YWY1ZGJkYmY3NjI1NGE2ZThhMGEyOTNjY2FlOWI2ODg7IHVkYl9kZXZpY2VpZD13Xzc2MTYyMzQ4Mzg2MTgxNTI5NjsgdWRiX3Bhc3NkYXRhPTM7IF9feWFzbWlkPTAuODAyOTkzNTA2NTAxMTI2OTsgSG1fbHZ0XzUxNzAwYjZjNzIyZjViYjRjZjM5OTA2YTU5NmVhNDFmPTE2OTY0MDk0MjAsMTY5NjQxMTgwNiwxNjk2NDczNzYwLDE2OTY0NzY3NDU7IF95YXNpZHM9X19yb290c2lkJTNEQ0E3NjY5MjcwM0IwMDAwMTNGRkJCRkVBNjY4Nzk5RDA7IEhtX2xwdnRfNTE3MDBiNmM3MjJmNWJiNGNmMzk5MDZhNTk2ZWE0MWY9MTY5NjQ3Njg4MDsgaHV5YV91YT13ZWJoNSYwLjAuMSZhY3Rpdml0eTsgX3JlcF9jbnQ9Mzsgc2RpZD0wVW5IVWd2MC9xbWZENEtBS2x3emhxU1drU1d3a3RzZXhQSWNsTnQzUGJ3TVFha1dOTmZrMlc4KzFkNlFNZzVZYnU1bC9GcDRpaHRUTDdCd0Q2bTQ0MENHUUNkU0htZW1kOUNNLzVKRVZkanJXVmtuOUx0ZkZKdy9RbzRrZ0tyOE9aSERxTm51d2c2MTJzR3lmbEZuMWRrVWVaWVRUb0N6emw0R0NIcTdNVURhaHhHdVBSOG1VZGRmSW1GdGpjY3MxOyBodXlhX2ZsYXNoX3JlcF9jbnQ9NTc7IHJlcF9jbnQ9NDA7IGh1eWFfd2ViX3JlcF9jbnQ9MTY2XGYGY2hyb21lCxoAAxwqFgAmADYARgBWAAsLIAELjJgMqAwsNiU5N2Q5NzQ4NTYyMzdiMWZiOjk3ZDk3NDg1NjIzN2IxZmI6MDowTFxmIGU2OTYzZjMwMTRmYzZlY2U2M2ExNmU3ZTlhMzMzOWVl");
        WupReq wupReq = new WupReq();
        wupReq.decode(decode);
        LiveLaunchReq liveLaunchReq = new LiveLaunchReq();
        liveLaunchReq = wupReq.getUniAttribute().getByClass("tReq", liveLaunchReq);
        UserId tId = liveLaunchReq.getTId();
    }

    @Test
    void decodeLiveLaunchRsp() {
        byte[] decode = Base64.decode("AAQdAAECXAAAAlwQAyw8QARWBmxpdmV1aWYIZG9MYXVuY2h9AAECNggAAgYAHQAAAQwGBHRSc3AdAAECIQoGIDBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3EmUeNXopAAUKAAEZAAQGETExMS4xOS4yMzkuMTExOjgwBhAxMTEuMTkuMjI1LjIzOjgwBhAxMTEuNjMuMTgwLjk4OjgwBhExMTEuNjMuMTgwLjEwMDo4MAsKAAUZAAQGFzZmMTNlZjZmLXdzLnZhLmh1eWEuY29tBhc2ZjEzZTExNy13cy52YS5odXlhLmNvbQYXNmYzZmI0NjItd3MudmEuaHV5YS5jb20GFzZmM2ZiNDY0LXdzLnZhLmh1eWEuY29tCwoABhkABAYRMTExLjE5LjIzOS4xMTE6ODAGEDExMS4xOS4yMjUuMjM6ODAGEDExMS42My4xODAuOTg6ODAGETExMS42My4xODAuMTAwOjgwCwoABxkABAYRMTExLjE5LjIzOS4xMTE6ODAGEDExMS4xOS4yMjUuMjM6ODAGEDExMS42My4xODAuOTg6ODAGETExMS42My4xODAuMTAwOjgwCwoACRkACAYOMTIwLjE5NS4xNTguNDYGDzExMy4xMDcuMjM2LjE5NQYMMTQuMTcuMTA5LjY2Bg8xMDMuMjI3LjEyMS4xMDAGDzExNS4yMzguMTg5LjIyNQYPMTgzLjIzMi4xMzYuMTMwBg4yMjEuMjI4Ljc5LjIyNQYMNjAuMjE3LjI1MC4xCzxGDTExMi40My45Mi4xMTgLjJgMqAwsNiU5N2Q5NzQ4NTYyMzdiMWZiOjk3ZDk3NDg1NjIzN2IxZmI6MDowTFxmAA==");
        WupRsp wupRsp = new WupRsp();
        wupRsp.decode(decode);
        LiveLaunchRsp liveLaunchRsp = new LiveLaunchRsp();
        liveLaunchRsp = wupRsp.getUniAttribute().getByClass("tRsp", liveLaunchRsp);
        int eAccess = liveLaunchRsp.getEAccess();
    }

    @Test
    void decodeGetLivingInfoReq() {
        byte[] decode = Base64.decode("AAMdAAEEKAAABCgQAyw8QAFWCmh1eWFsaXZldWlmDWdldExpdmluZ0luZm99AAED+QgAAQYEdFJlcR0AAQPrCgoMFiAwYTdkY2E3MmEzY2UxYjY1NDAwMWRkMmFkZTJhZTg1NyYANhp3ZWJoNSYyMzA5MjcxMTUyJndlYnNvY2tldEcAAAOKdnBsYXllcl9zYmFubmVyXzE3MjQ2OTFfMTcyNDY5MT0xOyBTb3VuZFZhbHVlPTAuNTA7IGFscGhhVmFsdWU9MC44MDsgZ2FtZV9kaWQ9UjI0SjJnMG1CenZkWEpmN2E5bmhlU2wzekljaTJCT3AwLXQ7IGlzSW5MaXZlUm9vbT10cnVlOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyBfX3lhbWlkX3R0MT0wLjgwMjk5MzUwNjUwMTEyNjk7IF9feWFtaWRfbmV3PUNBNzVENENENUMxMDAwMDExNjVCODJCNTIxNDBDOTAwOyBndWlkPTBhN2RjYTcyYTNjZTFiNjU0MDAxZGQyYWRlMmFlODU3OyB1ZGJfZ3VpZGRhdGE9YWY1ZGJkYmY3NjI1NGE2ZThhMGEyOTNjY2FlOWI2ODg7IHVkYl9kZXZpY2VpZD13Xzc2MTYyMzQ4Mzg2MTgxNTI5NjsgdWRiX3Bhc3NkYXRhPTM7IF9feWFzbWlkPTAuODAyOTkzNTA2NTAxMTI2OTsgX3lhc2lkcz1fX3Jvb3RzaWQlM0RDQTc2NzA0NUMxMTAwMDAxRUM1QTE0NUMxRTkwRkUwMDsgSG1fbHZ0XzUxNzAwYjZjNzIyZjViYjRjZjM5OTA2YTU5NmVhNDFmPTE2OTY0NzM3NjAsMTY5NjQ3Njc0NSwxNjk2NDgzNTY1LDE2OTY0ODQyMTI7IEhtX2xwdnRfNTE3MDBiNmM3MjJmNWJiNGNmMzk5MDZhNTk2ZWE0MWY9MTY5NjQ4NDIxMjsgaHV5YV91YT13ZWJoNSYwLjAuMSZhY3Rpdml0eTsgX3JlcF9jbnQ9Mjsgc2RpZD0wVW5IVWd2MC9xbWZENEtBS2x3emhxWDk4UXJuUENjY2s2Zk40OTRpYXdTNUtteW1ncmV1ODlvN2dZdGEvUXZRc0JEZVNlU1JwNC9nclE1ZkV1RkFZckw1OWNvQUt1eHV1a2l3Z1RMamZqRURXVmtuOUx0ZkZKdy9RbzRrZ0tyOE9aSERxTm51d2c2MTJzR3lmbEZuMWRrVWVaWVRUb0N6emw0R0NIcTdNVURhaHhHdVBSOG1VZGRmSW1GdGpjY3MxOyBodXlhX2ZsYXNoX3JlcF9jbnQ9NzQ7IGh1eWFfd2ViX3JlcF9jbnQ9MTI1OyByZXBfY250PTQ0XGYGY2hyb21lCxwsMgAaURNGAFYAbHyMC4yYDKgMLDYlZTcxMDQ2OTExYzk2N2JjNDplNzEwNDY5MTFjOTY3YmM0OjA6MExcZiBjNTRlM2NkYmIyNGJjYzcyYmU1MjU5NTY4ZGVmY2Q1Ng==");
        WupReq wupReq = new WupReq();
        wupReq.decode(decode);
        GetLivingInfoReq getLivingInfoReq = new GetLivingInfoReq();
        getLivingInfoReq = wupReq.getUniAttribute().getByClass("tReq", getLivingInfoReq);

        byte[] decode2 = Base64.decode("AAAEJxwsPEABVgpodXlhbGl2ZXVpZg1nZXRMaXZpbmdJbmZvfQABA/kIAAEGBHRSZXEdAAED6woKDBYgMGE3ZGNhNzJhM2NlMWI2NTQwMDFkZDJhZGUyYWU4NTcmADYad2ViaDUmMjMwOTI3MTE1MiZ3ZWJzb2NrZXRHAAADinZwbGF5ZXJfc2Jhbm5lcl8xNzI0NjkxXzE3MjQ2OTE9MTsgU291bmRWYWx1ZT0wLjUwOyBhbHBoYVZhbHVlPTAuODA7IGdhbWVfZGlkPVIyNEoyZzBtQnp2ZFhKZjdhOW5oZVNsM3pJY2kyQk9wMC10OyBpc0luTGl2ZVJvb209dHJ1ZTsgZ3VpZD0wYTdkY2E3MmEzY2UxYjY1NDAwMWRkMmFkZTJhZTg1NzsgX195YW1pZF90dDE9MC44MDI5OTM1MDY1MDExMjY5OyBfX3lhbWlkX25ldz1DQTc1RDRDRDVDMTAwMDAxMTY1QjgyQjUyMTQwQzkwMDsgZ3VpZD0wYTdkY2E3MmEzY2UxYjY1NDAwMWRkMmFkZTJhZTg1NzsgdWRiX2d1aWRkYXRhPWFmNWRiZGJmNzYyNTRhNmU4YTBhMjkzY2NhZTliNjg4OyB1ZGJfZGV2aWNlaWQ9d183NjE2MjM0ODM4NjE4MTUyOTY7IHVkYl9wYXNzZGF0YT0zOyBfX3lhc21pZD0wLjgwMjk5MzUwNjUwMTEyNjk7IF95YXNpZHM9X19yb290c2lkJTNEQ0E3NjcwNDVDMTEwMDAwMUVDNUExNDVDMUU5MEZFMDA7IEhtX2x2dF81MTcwMGI2YzcyMmY1YmI0Y2YzOTkwNmE1OTZlYTQxZj0xNjk2NDczNzYwLDE2OTY0NzY3NDUsMTY5NjQ4MzU2NSwxNjk2NDg0MjEyOyBIbV9scHZ0XzUxNzAwYjZjNzIyZjViYjRjZjM5OTA2YTU5NmVhNDFmPTE2OTY0ODQyMTI7IGh1eWFfdWE9d2ViaDUmMC4wLjEmYWN0aXZpdHk7IF9yZXBfY250PTI7IHNkaWQ9MFVuSFVndjAvcW1mRDRLQUtsd3pocVg5OFFyblBDY2NrNmZONDk0aWF3UzVLbXltZ3JldTg5bzdnWXRhL1F2UXNCRGVTZVNScDQvZ3JRNWZFdUZBWXJMNTljb0FLdXh1dWtpd2dUTGpmakVEV1ZrbjlMdGZGSncvUW80a2dLcjhPWkhEcU5udXdnNjEyc0d5ZmxGbjFka1VlWllUVG9DenpsNEdDSHE3TVVEYWh4R3VQUjhtVWRkZkltRnRqY2NzMTsgaHV5YV9mbGFzaF9yZXBfY250PTc0OyBodXlhX3dlYl9yZXBfY250PTEyNTsgcmVwX2NudD00NFxmBmNocm9tZQscLDIAGlETRgBWAGx8jAuMmAyoDA==");
        WupReq wupReq2 = new WupReq();
        wupReq2.decode(decode2);
        GetLivingInfoReq getLivingInfoReq2 = new GetLivingInfoReq();
        getLivingInfoReq2 = wupReq2.getUniAttribute().getByClass("tReq", getLivingInfoReq2);

        UserId tId = getLivingInfoReq.getTId();
    }
}
