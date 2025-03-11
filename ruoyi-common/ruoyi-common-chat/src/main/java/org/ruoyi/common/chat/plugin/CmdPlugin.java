package org.ruoyi.common.chat.plugin;

import org.ruoyi.common.chat.openai.plugin.PluginAbstract;

import java.io.IOException;

public class CmdPlugin extends PluginAbstract<CmdReq, CmdResp> {

    public CmdPlugin(Class<?> r) {
        super(r);
    }

    @Override
    public CmdResp func(CmdReq args) {
        try {
            if("计算器".equals(args.getCmd())){
                Runtime.getRuntime().exec("calc");
            }else if("记事本".equals(args.getCmd())){
                Runtime.getRuntime().exec("notepad");
            }else if("命令行".equals(args.getCmd())){
                String [] cmd={"cmd","/C","start copy exel exe2"};
                Runtime.getRuntime().exec(cmd);
            }
        } catch (IOException e) {
           throw new RuntimeException("指令执行失败");
        }
        CmdResp resp = new CmdResp();
        resp.setResult(args.getCmd()+"指令执行成功!");
        return resp;
    }

    @Override
    public String content(CmdResp resp) {
        return resp.getResult();
    }
}
