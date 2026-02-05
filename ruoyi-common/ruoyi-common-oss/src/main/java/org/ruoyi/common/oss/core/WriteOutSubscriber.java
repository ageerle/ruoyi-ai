package org.ruoyi.common.oss.core;

import java.io.IOException;

/**
 * 写出订阅器
 *
 * @author 秋辞未寒
 */
@FunctionalInterface
public interface WriteOutSubscriber<T> {

    void writeTo(T out) throws IOException;

}
