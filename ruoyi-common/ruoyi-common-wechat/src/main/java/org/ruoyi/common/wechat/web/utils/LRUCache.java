package org.ruoyi.common.wechat.web.utils;

import java.util.LinkedHashMap;

/**
 * @author WesleyOne
 * @create 2018/12/12
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {

    private static final long serialVersionUID = 1L;
    protected int maxElements;

    public LRUCache(int maxSize)
    {
        super(maxSize * 4 / 3 , 0.75F, true);
        maxElements = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry eldest)
    {
        return size() > maxElements;
    }

}
