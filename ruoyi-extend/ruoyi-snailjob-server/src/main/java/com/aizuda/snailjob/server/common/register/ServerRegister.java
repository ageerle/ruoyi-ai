package com.aizuda.snailjob.server.common.register;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.common.core.enums.NodeTypeEnum;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.common.core.util.NetUtil;
import com.aizuda.snailjob.common.core.util.SnailJobVersion;
import com.aizuda.snailjob.common.core.util.StreamUtils;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.server.common.cache.CacheConsumerGroup;
import com.aizuda.snailjob.server.common.config.SystemProperties;
import com.aizuda.snailjob.server.common.convert.RegisterNodeInfoConverter;
import com.aizuda.snailjob.server.common.dto.ServerNodeExtAttrs;
import com.aizuda.snailjob.server.common.handler.InstanceManager;
import com.aizuda.snailjob.template.datasource.persistence.po.ServerNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务端注册
 *
 * @author opensnail
 * @date 2023-06-07
 * @since 1.6.0
 */
@Component(ServerRegister.BEAN_NAME)
@RequiredArgsConstructor
public class ServerRegister extends AbstractRegister {
    public static final String BEAN_NAME = "serverRegister";
    private final ScheduledExecutorService serverRegisterNode = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "server-register-node"));
    public static final int DELAY_TIME = 30;
    public static final String CURRENT_CID;
    public static final String GROUP_NAME = "DEFAULT_SERVER";
    public static final String NAMESPACE_ID = "DEFAULT_SERVER_NAMESPACE_ID";
    private final InstanceManager instanceManager;
    private final SystemProperties systemProperties;
    private final ServerProperties serverProperties;

    static {
        CURRENT_CID = IdUtil.getSnowflakeNextIdStr();
    }

    @Override
    public boolean supports(int type) {
        return getNodeType().equals(type);
    }

    @Override
    protected void beforeProcessor(RegisterContext context) {
        // 新增扩展参数
        ServerNodeExtAttrs serverNodeExtAttrs = new ServerNodeExtAttrs();
        serverNodeExtAttrs.setWebPort(serverProperties.getPort());
        serverNodeExtAttrs.setSystemVersion(SnailJobVersion.getVersion());

        context.setGroupName(GROUP_NAME);
        context.setHostId(CURRENT_CID);
        String serverHost = systemProperties.getServerHost();
        if (StrUtil.isEmptyIfStr(serverHost)) {
            serverHost = NetUtil.getLocalIpStr();
        }
        context.setHostIp(serverHost);
        context.setHostPort(systemProperties.getServerPort());
        context.setContextPath(Optional.ofNullable(serverProperties.getServlet().getContextPath()).orElse(StrUtil.EMPTY));
        context.setNamespaceId(NAMESPACE_ID);
        context.setExtAttrs(JsonUtil.toJsonString(serverNodeExtAttrs));
    }

    @Override
    protected LocalDateTime getExpireAt() {
        return LocalDateTime.now().plusSeconds(DELAY_TIME);
    }

    @Override
    protected boolean doRegister(RegisterContext context, ServerNode serverNode) {
        refreshExpireAt(Lists.newArrayList(serverNode));
        return Boolean.TRUE;
    }


    @Override
    protected void afterProcessor(final ServerNode serverNode) {
        try {
            // 同步当前POD消费的组的节点信息
            // netty的client只会注册到一个服务端，若组分配的和client连接的不是一个POD则会导致当前POD没有其他客户端的注册信息
            ConcurrentMap<String /*groupName*/, Set<String>/*namespaceId*/> allConsumerGroupName = CacheConsumerGroup.getAllConsumerGroupName();
            if (CollUtil.isNotEmpty(allConsumerGroupName)) {
                Set<String> namespaceIdSets = StreamUtils.toSetByFlatMap(allConsumerGroupName.values(), Set::stream);
                if (CollUtil.isEmpty(namespaceIdSets)) {
                    return;
                }

                List<ServerNode> serverNodes = serverNodeMapper.selectList(
                        new LambdaQueryWrapper<ServerNode>()
                                .eq(ServerNode::getNodeType, NodeTypeEnum.CLIENT.getType())
                                .in(ServerNode::getNamespaceId, namespaceIdSets)
                                .in(ServerNode::getGroupName, allConsumerGroupName.keySet()));
                for (final ServerNode node : serverNodes) {
                    // 刷新全量本地缓存
                    instanceManager.registerOrUpdate(RegisterNodeInfoConverter.INSTANCE.toRegisterNodeInfo(node));
                    // 刷新过期时间
                    CacheConsumerGroup.addOrUpdate(node.getGroupName(), node.getNamespaceId());
                }
            }
        } catch (Exception e) {
            SnailJobLog.LOCAL.error("Client refresh failed", e);
        }
    }

    @Override
    protected Integer getNodeType() {
        return NodeTypeEnum.SERVER.getType();
    }

    @Override
    public void start() {
        SnailJobLog.LOCAL.info("ServerRegister start");

        serverRegisterNode.scheduleAtFixedRate(() -> {
            try {
                this.register(new RegisterContext());
            } catch (Exception e) {
                SnailJobLog.LOCAL.error("Server-side registration failed", e);
            }
        }, 0, DELAY_TIME * 2 / 3, TimeUnit.SECONDS);

    }

    @Override
    public void close() {
        SnailJobLog.LOCAL.info("ServerRegister close");
    }
}
