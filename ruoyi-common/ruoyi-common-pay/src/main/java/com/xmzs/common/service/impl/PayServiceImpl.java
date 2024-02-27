package com.xmzs.common.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.xmzs.common.config.PayConfig;
import com.xmzs.common.service.PayService;
import com.xmzs.common.utils.MD5Util;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务
 * @author Admin
 */
@Service
public class PayServiceImpl implements PayService {
    @Override
    public String getPayUrl(String orderNo, String name, double money, String clientIp) {
        String out_trade_no = orderNo, sign = "";
        //封装请求参数
        String mdString = "clientip=" + clientIp + "&device=" + PayConfig.device + "&money=" + money + "&name=" + name + "&" +
            "notify_url=" + PayConfig.notify_url + "&out_trade_no=" + out_trade_no + "&pid=" + PayConfig.pid + "&return_url=" + PayConfig.return_url +
            "&type=" + PayConfig.type + PayConfig.key;
        sign = MD5Util.GetMD5Code(mdString);
        Map<String, Object> map = new HashMap<>(10);
        map.put("clientip", clientIp);
        map.put("device", PayConfig.device);
        map.put("money", money);
        map.put("name", name);
        map.put("notify_url", PayConfig.notify_url);
        map.put("out_trade_no", out_trade_no);
        map.put("pid", PayConfig.pid);
        map.put("return_url", PayConfig.return_url);
        map.put("sign_type", PayConfig.sign_type);
        map.put("type", PayConfig.type);
        map.put("sign", sign);
        String body = HttpUtil.post(PayConfig.payUrl, map);
        JSONObject jsonObject = new JSONObject(body);
        return (String) jsonObject.get("qrcode");
    }
}
