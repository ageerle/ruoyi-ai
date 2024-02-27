/*
 * MIT License
 *
 * Copyright (c) 2023 OrdinaryRoad
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tech.ordinaryroad.live.chat.client.douyu.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatReflectUtil;
import tech.ordinaryroad.live.chat.client.douyu.api.DouyuApis;
import tech.ordinaryroad.live.chat.client.douyu.client.DouyuLiveChatClient;
import tech.ordinaryroad.live.chat.client.douyu.constant.DouyuCmdEnum;
import tech.ordinaryroad.live.chat.client.douyu.msg.DgbMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.DouyuCmdMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.HeartbeatMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.HeartbeatReplyMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.BaseDouyuCmdMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.base.IDouyuMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.dto.GiftListInfo;
import tech.ordinaryroad.live.chat.client.douyu.msg.dto.GiftPropSingle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 参考：https://open.douyu.com/source/api/63
 *
 * @author mjz
 * @date 2023/1/6
 */
@Slf4j
public class DouyuCodecUtil {

    public static final String[] IGNORE_PROPERTIES = {"OBJECT_MAPPER", "unknownProperties"};
    /**
     * 以SHOULD_IGNORE开头的成员变量将不会序列化
     */
    public static final String SHOULD_IGNORE_PROPERTIES_PREFIX = "SHOULD_IGNORE";

    public static final short MSG_TYPE_SEND = 689;
    public static final short MSG_TYPE_RECEIVE = 690;
    public static final short FRAME_HEADER_LENGTH = 8;

    public static ByteBuf encode(BaseDouyuCmdMsg msg, List<String> containProperties) {
        ByteBuf out = Unpooled.buffer(FRAME_HEADER_LENGTH);
        String bodyDouyuSttString = StrUtil.nullToEmpty(toDouyuSttString(msg, containProperties)) + SUFFIX;
        byte[] bodyBytes = bodyDouyuSttString.getBytes(StandardCharsets.UTF_8);
        int length = bodyBytes.length + FRAME_HEADER_LENGTH;
        out.writeIntLE(length);
        out.writeIntLE(length);
        out.writeShortLE(MSG_TYPE_SEND);
        out.writeByte(0);
        out.writeByte(0);
        out.writeBytes(bodyBytes);
        return out;
    }

    public static ByteBuf encode(BaseDouyuCmdMsg msg) {
        return encode(msg, null);
    }

    public static List<IDouyuMsg> decode(ByteBuf in) {
        List<IDouyuMsg> msgList = new ArrayList<>();
        Queue<ByteBuf> pendingByteBuf = new LinkedList<>();

        do {
            Optional<IDouyuMsg> msg = doDecode(in, pendingByteBuf);
            msg.ifPresent(msgList::add);
            in = pendingByteBuf.poll();
        } while (in != null);

        return msgList;
    }

    /**
     * 执行解码操作
     *
     * @param in             handler收到的一条消息
     * @param pendingByteBuf 用于存放未读取完的ByteBuf
     * @return Optional<IDouyuMsg> 何时为空值：不支持的{@link DouyuCmdEnum}，{@link #parseDouyuSttString(String, short)}反序列化失败
     */
    private static Optional<IDouyuMsg> doDecode(ByteBuf in, Queue<ByteBuf> pendingByteBuf) {
        int length = in.readIntLE();
        in.readIntLE();
        // MSG_TYPE_RECEIVE
        short msgType = in.readShortLE();
        if (msgType != MSG_TYPE_RECEIVE) {
            log.error("decode消息类型 非 收到的消息");
        }
        in.readByte();
        in.readByte();
        int contentLength = length - FRAME_HEADER_LENGTH;
        byte[] inputBytes = new byte[contentLength];
        in.readBytes(inputBytes);
        if (in.readableBytes() != 0) {
            // log.error("in.readableBytes() {}", in.readableBytes());
            pendingByteBuf.offer(in);
        }

        String bodyDouyuSttString = new String(inputBytes, 0, inputBytes.length - 1);
        return Optional.ofNullable(parseDouyuSttString(bodyDouyuSttString, msgType));
    }

    public static final String SPLITTER = "@=";
    public static final String END = "/";
    public static final String SUFFIX = "\0";

    /**
     * <pre>{@code @S/ -> @AS@S}</pre>
     *
     * @param string
     * @return
     */
    public static String escape(String string) {
//        return string == null ? StrUtil.EMPTY : (string.replaceAll("/", "@S").replaceAll("@", "@A"));
        return string == null ? StrUtil.EMPTY : (string.replaceAll("@", "@A").replaceAll("/", "@S"));
    }

    /**
     * <pre>{@code @AS@S -> @S/}</pre>
     *
     * @param string
     * @return
     */
    public static String unescape(String string) {
        return string == null ? StrUtil.EMPTY : (string.replaceAll("@S", "/").replaceAll("@A", "@"));
    }

    public static String toDouyuSttString(Object object, List<String> containProperties) {
        StringBuffer sb = new StringBuffer();
        if (object instanceof IDouyuMsg) {
            Class<?> objectClass = object.getClass();
            Field[] fields = ReflectUtil.getFields(objectClass, field -> {
                String name = field.getName();
                if (CollUtil.isNotEmpty(containProperties)) {
                    return containProperties.contains(name);
                } else {
                    return !name.startsWith(SHOULD_IGNORE_PROPERTIES_PREFIX) && !ArrayUtil.contains(IGNORE_PROPERTIES, name);
                }
            });
            for (Field field : fields) {
                String key = field.getName();
                Method method = OrLiveChatReflectUtil.getGetterMethod(objectClass, key);
                Object value = ReflectUtil.invoke(object, method);
                String douyuSttString = toDouyuSttString(value, containProperties);
                String escape = escape(douyuSttString);
                sb.append(escape(key))
                        .append(SPLITTER)
                        .append(escape)
                        .append(END);
            }
        } else {
            if (object instanceof Iterable<?>) {
                Iterable<?> iterable = (Iterable<?>) object;
                StringBuffer iterableStringBuffer = new StringBuffer();
                for (Object o : iterable) {
                    String douyuSttString = toDouyuSttString(o, containProperties);
                    String escape = escape(douyuSttString);
                    iterableStringBuffer.append(escape)
                            .append(END);
                }
                sb.append((iterableStringBuffer.toString()));
            } else if (object instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) object;
                StringBuffer mapStringBuffer = new StringBuffer();
                map.forEach((mapKey, mapValue) -> {
                    mapStringBuffer.append(escape(StrUtil.toStringOrNull(mapKey)))
                            .append(SPLITTER)
                            .append(escape(toDouyuSttString(mapValue, containProperties)))
                            .append(END);
                });
                sb.append((mapStringBuffer.toString()));
            } else {
                sb.append((StrUtil.nullToEmpty(StrUtil.toStringOrNull(object))));
            }
        }
        return sb.toString();
    }

    public static String toDouyuSttString(Object object) {
        return toDouyuSttString(object, null);
    }

    public static IDouyuMsg parseDouyuSttString(String string, short msgType) {
        Map<String, Object> stringObjectMap = parseDouyuSttStringToMap(string);
        String type = (String) stringObjectMap.get("type");
        DouyuCmdEnum cmdEnum = DouyuCmdEnum.getByString(type);

        Class<IDouyuMsg> msgClass = getDouyuMsgClassByType(cmdEnum, msgType);
        if (msgClass == null) {
            // TODO 不支持的cmdEnum
            if (log.isWarnEnabled()) {
                log.warn("暂不支持 cmdEnum {}, msgType {}", cmdEnum, msgType);
            }
            return null;
        }

        IDouyuMsg t = ReflectUtil.newInstance(msgClass);
        stringObjectMap.forEach((key, value) -> {
            Field field = ReflectUtil.getField(t.getClass(), key);
            // 未知key
            if (field == null) {
                // Object -> JsonNode
                ((BaseDouyuCmdMsg) t).getUnknownProperties().put(key, BaseDouyuCmdMsg.OBJECT_MAPPER.valueToTree(value));
                // log.debug("未知key {} {}，已存放于unknownProperties中", msgClass, key);
            } else {
                ReflectUtil.setFieldValue(t, field, value);
            }
        });

        // 礼物消息设置礼物信息字段
        if (t instanceof DgbMsg) {
            DgbMsg msg = (DgbMsg) t;
            String pid = msg.getPid();
            // 通用礼物
            if (StrUtil.isNotBlank(pid)) {
                GiftPropSingle giftSingle = DouyuLiveChatClient.giftMap.get(pid, () -> {
                    GiftPropSingle gift = GiftPropSingle.DEFAULT_GIFT;
                    try {
                        gift = DouyuApis.getGiftPropSingleByPid(pid);
                    } catch (Exception e) {
                        log.error("礼物信息获取失败, pid=" + pid, e);
                    }
                    return gift;
                });
                msg.setGiftInfo(giftSingle);
            }
            // 房间礼物
            else {
                String realRoomId = msg.getRid();
                if (DouyuLiveChatClient.roomGiftMap.containsKey(realRoomId)) {
                    Map<String, GiftListInfo> stringGiftListInfoMap = DouyuLiveChatClient.roomGiftMap.get(realRoomId);
                    msg.setRoomGiftInfo(stringGiftListInfoMap.getOrDefault(String.valueOf(msg.getGfid()), GiftListInfo.DEFAULT_GIFT));
                }
            }
        }

        return t;
    }

    public static Object parseDouyuSttStringToObject(String value) {
        Object valueObject;
        if (StrUtil.isBlank(value)) {
            return null;
        }
        if (value.contains(SPLITTER) && value.contains(END)) {
            // log.debug("map valueObject {}", value);
            valueObject = parseDouyuSttStringToMap(value);
        }
        // List<Object>
        else if (!value.contains(SPLITTER) && value.contains(END)) {
            // log.debug("list valueObject {}", value);
            List<Object> list = new ArrayList<>();
            for (String s : value.split(END)) {
                list.add(parseDouyuSttStringToObject(unescape(s)));
            }
            valueObject = list;
        }
        // String
        else {
            valueObject = value;
        }
        return valueObject;
    }

    public static Map<String, Object> parseDouyuSttStringToMap(String string) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        for (String s : string.split(END)) {
            String[] entry = s.split(SPLITTER);
            String key = unescape(entry[0]);
            String value = unescape(ArrayUtil.get(entry, 1));
            Object valueObject = parseDouyuSttStringToObject(value);
            stringObjectMap.put(key, valueObject);
        }
        return stringObjectMap;
    }

    public static <T extends IDouyuMsg> Class<T> getDouyuMsgClassByType(DouyuCmdEnum douyuCmdEnum, short msgType) {
        if (douyuCmdEnum == null) {
            return (Class<T>) DouyuCmdMsg.class;
        }

        Class<?> msgClass;
        Class<?> tClass = douyuCmdEnum.getTClass();
        if (tClass == null) {
            if (douyuCmdEnum == DouyuCmdEnum.mrkl) {
                if (msgType == MSG_TYPE_RECEIVE) {
                    msgClass = HeartbeatReplyMsg.class;
                } else if (msgType == MSG_TYPE_SEND) {
                    msgClass = HeartbeatMsg.class;
                } else {
                    msgClass = null;
                }
            } else {
                msgClass = DouyuCmdMsg.class;
            }
        } else {
            msgClass = tClass;
        }
        return (Class<T>) msgClass;
    }
}
