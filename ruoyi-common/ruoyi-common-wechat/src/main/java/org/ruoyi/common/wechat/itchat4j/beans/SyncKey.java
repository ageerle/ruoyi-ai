package org.ruoyi.common.wechat.itchat4j.beans;

import java.util.List;

/**
 * @author WesleyOne
 * @create 2018/12/21
 */
@Deprecated
public class SyncKey {

    private Integer Count;

    private List<KV> List;

    public Integer getCount() {
        return Count;
    }

    public void setCount(Integer count) {
        Count = count;
    }

    public java.util.List<KV> getList() {
        return List;
    }

    public void setList(java.util.List<KV> list) {
        List = list;
    }

    class KV{
        private Integer Key;
        private Long Val;

        public Integer getKey() {
            return Key;
        }

        public void setKey(Integer key) {
            Key = key;
        }

        public Long getVal() {
            return Val;
        }

        public void setVal(Long val) {
            Val = val;
        }
    }
}
