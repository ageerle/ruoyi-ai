package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.dto.GuestDemandSubmitReq;
import org.ruoyi.ipd.dto.GuestDemandSubmittedView;
import org.ruoyi.ipd.dto.GuestDemandUpdateReq;
import org.ruoyi.ipd.dto.GuestDemandView;
import org.ruoyi.ipd.dto.PublicProductView;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * P4-1.1 游客需求提交模型与三路产品归属（页38；BR-REQ-01/02/02b/03/04）。
 * <ul>
 *   <li>免登录提交仅最小公开字段；响应仅返回 8 位查询码（^[A-Z0-9]{8}$），不暴露内部 ID / 人员。</li>
 *   <li>三路归属：productId 非空且产品在售（ACTIVE）→ 按「产品 1:1 项目」的在职 MARKET_PM/RD_PM 写双 PM（BR-REQ-04）；
 *       「其他/不确定」→ product_id=NULL 进入待指派池，不路由不通知（AC-PROD-08）。</li>
 *   <li>服务端校验 + honeypot 拒绝（audit action=spam_rejected）+ 同 IP 每小时 10 次限流（第 11 次 40011）。</li>
 *   <li>审计 entityType=guest_demand，action=submit，detail 含 ipHash/uaHash（不落原始 IP/UA）。</li>
 * </ul>
 */
@Service
public class GuestDemandService {

    /** BR-REQ-03：8 位大写字母+数字（base36）。 */
    public static final Pattern QUERY_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
    /** 36^8 查询码空间。 */
    private static final long CODE_SPACE = 2_821_109_907_456L;
    private static final char[] BASE36 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_RETRY = 5;
    /** 页38：同一 IP 1 小时内最多 10 次提交。 */
    static final int RATE_LIMIT_PER_HOUR = 10;

    private final RequirementMapper requirementMapper;
    private final ProductMapper productMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final AuditLogService auditLogService;
    private final GuestRateLimiter rateLimiter;

    /** 限流抽象：默认内存滑动窗口（单实例）；多实例部署时替换为 Redis 实现，勿动调用方。 */
    public interface GuestRateLimiter {
        boolean tryAcquire(String ipHash);
    }

    /** Spring 注入用构造器；5 参重载留给测试注入自定义限流器，多构造器必须显式指定，否则上下文无法实例化。 */
    @Autowired
    public GuestDemandService(RequirementMapper requirementMapper,
                              ProductMapper productMapper,
                              ProjectMemberMapper projectMemberMapper,
                              AuditLogService auditLogService) {
        this(requirementMapper, productMapper, projectMemberMapper, auditLogService, new InMemoryHourRateLimiter());
    }

    public GuestDemandService(RequirementMapper requirementMapper,
                              ProductMapper productMapper,
                              ProjectMemberMapper projectMemberMapper,
                              AuditLogService auditLogService,
                              GuestRateLimiter rateLimiter) {
        this.requirementMapper = requirementMapper;
        this.productMapper = productMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.auditLogService = auditLogService;
        this.rateLimiter = rateLimiter;
    }

    public GuestDemandSubmittedView submit(GuestDemandSubmitReq req, String clientIp, String userAgent) {
        String ipHash = sha256Short(clientIp);
        String uaHash = sha256Short(userAgent);
        // honeypot：正常用户不可见，非空即机器人 → 400 + spam_rejected 审计（页38 用例4）
        if (req.website() != null && !req.website().isBlank()) {
            audit("spam_rejected", null, ipHash, uaHash, "honeypot filled");
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        validate(req);
        if (!rateLimiter.tryAcquire(ipHash)) {
            throw new IpdBusinessException(ApiV1ErrorCode.RATE_LIMITED);
        }
        Requirement r = new Requirement();
        r.setSource("PORTAL_GUEST");
        r.setSubmitterName(req.feedbackPerson());
        r.setCustomerName(req.customerName());
        r.setContact(req.contact());
        r.setRawModel(blankToNull(req.rawModel()));
        r.setTitle(buildTitle(r.getRawModel(), req.customerName()));
        r.setContent(req.functionalRequirement());
        r.setStatus("SUBMITTED");
        String route = "unassigned"; // AC-PROD-08：其他/不确定 → 待指派池，不路由
        if (req.productId() != null) {
            Product p = productMapper.selectById(req.productId());
            if (p == null) {
                throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
            }
            if (!"ACTIVE".equals(p.getStatus())) {
                // 页38：40401 产品已下架
                throw new IpdBusinessException(ApiV1ErrorCode.PRODUCT_INACTIVE);
            }
            r.setProductId(p.getId());
            route = resolveDualPm(p, r);
        }
        r.setQueryCode(generateUniqueCode());
        requirementMapper.insert(r);
        audit("submit", r.getId(), ipHash, uaHash,
            "route=" + route + ";productId=" + r.getProductId() + ";mkt=" + r.getMarketPmId() + ";rd=" + r.getRdPmId());
        return new GuestDemandSubmittedView(r.getQueryCode(), r.getStatus());
    }

    /** 页38：GET /api/public/products——返回 status 并派生 listingStatus（ON_SALE/IN_DEV/OTHER）供三情形选择。 */
    public List<PublicProductView> publicProducts() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ACTIVE")
                .orderByAsc(Product::getProductName))
            .stream()
            .map(p -> new PublicProductView(p.getId(), p.getProductName(), p.getModelCode(), p.getStatus(),
                deriveListingStatus(p)))
            .toList();
    }

    /**
     * P4-1.2：游客凭查询码查进度（AC-REQ-04；页39）。
     * <p>BR-REQ-03：仅返回 queryCode + 状态 + 路由时间 + 标题摘要；不暴露内部 ID / 人员 / 备注。
     * 不存在的查询码一律 NOT_FOUND，不区分大小写不泄露。
     */
    @Transactional(readOnly = true)
    public GuestDemandView queryByCode(String queryCode) {
        validateQueryCode(queryCode);
        Requirement r = requirementMapper.selectOne(new LambdaQueryWrapper<Requirement>()
            .eq(Requirement::getQueryCode, queryCode));
        if (r == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return toView(r);
    }

    /**
     * P4-1.2：受理前补登内容/联系方式（AC-REQ-04；页39 用例1）。
     * <p>BR-REQ-03a：受理后状态机不允许再改原文（仅可评论）；受理前可继续补充。
     * 受理标志：status 离开 SUBMITTED。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuestDemandView supplement(String queryCode, GuestDemandUpdateReq req, String clientIp, String userAgent) {
        validateQueryCode(queryCode);
        if (req == null || !GuestDemandUpdateReq.ACTION_SUPPLEMENT.equals(req.action())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.functionalRequirement() == null && req.contact() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.functionalRequirement() != null && req.functionalRequirement().length() < 6) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        Requirement r = requireByCode(queryCode);
        if (!"SUBMITTED".equals(r.getStatus())) {
            // 受理后锁定：原 spec 「需求已被受理，如需变更请追加评论」
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        if (req.functionalRequirement() != null) {
            r.setContent(req.functionalRequirement());
        }
        if (req.contact() != null) {
            r.setContact(req.contact());
        }
        requirementMapper.updateById(r);
        auditGuestAction("supplement", r.getId(), clientIp, userAgent,
            "fields=" + describeFields(req));
        return toView(r);
    }

    /**
     * P4-1.2：受理前撤回（AC-REQ-04 / AC-REQ-04b；页39 用例2）。
     * <p>BR-REQ-03b：仅 SUBMITTED 状态可撤回；受理后拒绝。
     * 撤回后状态置 WITHDRAWN（终态），查询码保留可查历史。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuestDemandView withdraw(String queryCode, GuestDemandUpdateReq req, String clientIp, String userAgent) {
        validateQueryCode(queryCode);
        if (req == null || !GuestDemandUpdateReq.ACTION_WITHDRAW.equals(req.action())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        Requirement r = requireByCode(queryCode);
        if (!"SUBMITTED".equals(r.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        r.setStatus("WITHDRAWN");
        requirementMapper.updateById(r);
        auditGuestAction("withdraw", r.getId(), clientIp, userAgent, "from=SUBMITTED;to=WITHDRAWN");
        return toView(r);
    }

    /**
     * P4-1.3：路由后回写双 PM（AC-REQ-03；页40）。
     * <p>BR-REQ-04：产品 1:1 项目（uk_products_project），取该项目在职 MARKET_PM/RD_PM 写双 PM，
     * 并发场景同 IP 同时双提同产品时走 MySQL 行锁/条件 UPDATE 守卫。
     * 仅 SUBMITTED/UNASSIGNED 状态可路由；已路由/已受理/已撤回不再覆盖。
     */
    @Transactional(rollbackFor = Exception.class)
    public GuestDemandView routeDualPm(String queryCode) {
        validateQueryCode(queryCode);
        Requirement r = requireByCode(queryCode);
        if ("WITHDRAWN".equals(r.getStatus()) || "ACCEPTED".equals(r.getStatus())
            || "CLOSED".equals(r.getStatus()) || "ARCHIVED".equals(r.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        if (r.getMarketPmId() != null && r.getRdPmId() != null) {
            // 已路由：幂等返回当前视图
            return toView(r);
        }
        if (r.getProductId() == null) {
            // 「其他/不确定」不进路由
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        Product p = productMapper.selectById(r.getProductId());
        if (p == null || !"ACTIVE".equals(p.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PRODUCT_INACTIVE);
        }
        String route = resolveDualPm(p, r);
        if (!"routed".equals(route)) {
            // 项目无在职 PM：不强行写空，回退到 UNASSIGNED 状态供超管指派
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        // 条件 UPDATE 守卫：仅 SUBMITTED/UNASSIGNED 才更新
        int rows = requirementMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Requirement>()
            .eq(Requirement::getId, r.getId())
            .eq(Requirement::getStatus, "SUBMITTED")
            .or().eq(Requirement::getStatus, "UNASSIGNED")
            .set(Requirement::getMarketPmId, r.getMarketPmId())
            .set(Requirement::getRdPmId, r.getRdPmId())
            .set(Requirement::getRoutedAt, r.getRoutedAt()));
        if (rows == 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        auditGuestAction("route_dual_pm", r.getId(), null, null,
            "mkt=" + r.getMarketPmId() + ";rd=" + r.getRdPmId());
        return toView(r);
    }

    /**
     * P4-1.3：扫描待指派需求并按 AC-PROD-09 兜底（5 工作日超时未处理通知超管）。
     * <p>BR-REQ-04a：submitedAt + 5 个工作日（跳过周末）后仍未处理，通知超管。
     * 由 scheduler 周期调用；本方法做幂等扫描，写 notification_events。
     */
    @Transactional(rollbackFor = Exception.class)
    public int notifyOverdueUnassigned() {
        Date threshold = subtractBusinessDays(new Date(), 5);
        List<Requirement> overdue = requirementMapper.selectList(new LambdaQueryWrapper<Requirement>()
            .eq(Requirement::getStatus, "SUBMITTED")
            .isNull(Requirement::getMarketPmId)
            .isNull(Requirement::getRdPmId)
            .le(Requirement::getCreateTime, threshold));
        int notified = 0;
        for (Requirement r : overdue) {
            // 调用方注入 NotificationService 不可行（解耦约束），此处落 audit 留痕；
            // 真实通知由 scheduler 在 audit 后调用 publish（见后续 P2-4.1 桥接）
            auditGuestAction("overdue_unassigned", r.getId(), null, null,
                "threshold=" + threshold.getTime() + ";createTime=" + r.getCreateTime().getTime());
            notified++;
        }
        return notified;
    }

    private GuestDemandView toView(Requirement r) {
        String titleSummary = r.getTitle() == null ? null
            : (r.getTitle().length() > 60 ? r.getTitle().substring(0, 60) + "..." : r.getTitle());
        return new GuestDemandView(r.getQueryCode(), r.getStatus(),
            r.getAcceptedAt(), r.getRoutedAt(), titleSummary);
    }

    private Requirement requireByCode(String queryCode) {
        Requirement r = requirementMapper.selectOne(new LambdaQueryWrapper<Requirement>()
            .eq(Requirement::getQueryCode, queryCode));
        if (r == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return r;
    }

    private void validateQueryCode(String queryCode) {
        if (queryCode == null || !QUERY_CODE_PATTERN.matcher(queryCode).matches()) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
    }

    private static String describeFields(GuestDemandUpdateReq req) {
        StringBuilder sb = new StringBuilder();
        if (req.functionalRequirement() != null) {
            sb.append("content;");
        }
        if (req.contact() != null) {
            sb.append("contact;");
        }
        return sb.toString();
    }

    /** 5 个工作日（跳过周六/周日）的回退；用于 AC-PROD-09 兜底判定。 */
    static Date subtractBusinessDays(Date from, int bizDays) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(from);
        int subtracted = 0;
        while (subtracted < bizDays) {
            c.add(java.util.Calendar.DAY_OF_MONTH, -1);
            int dow = c.get(java.util.Calendar.DAY_OF_WEEK);
            if (dow != java.util.Calendar.SATURDAY && dow != java.util.Calendar.SUNDAY) {
                subtracted++;
            }
        }
        return c.getTime();
    }

    private void auditGuestAction(String action, Long entityId, String ipHash, String uaHash, String detail) {
        auditLogService.append(AuditLog.builder()
            .operatorName("GUEST").operatorRole("GUEST")
            .action(action).entityType("guest_demand").entityId(entityId)
            .ipAddress(ipHash)
            .afterData(AuditEventData.json("detail",
                "ipHash=" + (ipHash == null ? "" : ipHash)
                    + ";uaHash=" + (uaHash == null ? "" : uaHash) + ";" + detail))
            .build());
    }

    /** BR-REQ-04：产品 1:1 项目（uk_products_project），取该项目在职 MARKET_PM/RD_PM 写双 PM。 */
    private String resolveDualPm(Product p, Requirement r) {
        if (p.getProjectId() == null) {
            return "no-project";
        }
        // 防御拷贝后排序：不原地修改 mapper 返回的列表
        List<ProjectMember> members = new ArrayList<>(projectMemberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, p.getProjectId())
            .isNull(ProjectMember::getExitDate)
            .in(ProjectMember::getRole, "MARKET_PM", "RD_PM")));
        members.sort(Comparator.comparing(ProjectMember::getJoinDate,
            Comparator.nullsLast(Comparator.naturalOrder())));
        boolean routed = false;
        for (ProjectMember m : members) {
            if ("MARKET_PM".equals(m.getRole()) && r.getMarketPmId() == null) {
                r.setMarketPmId(m.getPersonId());
                routed = true;
            } else if ("RD_PM".equals(m.getRole()) && r.getRdPmId() == null) {
                r.setRdPmId(m.getPersonId());
                routed = true;
            }
        }
        if (routed) {
            r.setRoutedAt(new Date());
            return "routed";
        }
        return "project-no-pm";
    }

    private void validate(GuestDemandSubmitReq req) {
        if (req.customerName() == null || req.customerName().length() < 2 || req.customerName().length() > 120) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        // 上限对齐列宽 submitter_name varchar(64)，防 Data too long
        if (req.feedbackPerson() == null || req.feedbackPerson().length() < 2 || req.feedbackPerson().length() > 64) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.contact() != null && req.contact().length() > 128) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.rawModel() != null && req.rawModel().length() > 64) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.functionalRequirement() == null || req.functionalRequirement().length() < 6
            || req.functionalRequirement().length() > 4000) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    /** 查询码：[0,36^8) 随机数定长 8 位 base36 大写；uk_req_query_code 冲突重试。 */
    private String generateUniqueCode() {
        for (int i = 0; i < CODE_RETRY; i++) {
            String code = randomCode();
            Long dup = requirementMapper.selectCount(new LambdaQueryWrapper<Requirement>()
                .eq(Requirement::getQueryCode, code));
            if (dup == null || dup == 0) {
                return code;
            }
        }
        throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
    }

    static String randomCode() {
        long v = Math.floorMod(RANDOM.nextLong(), CODE_SPACE);
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(BASE36[(int) (v % 36)]);
            v /= 36;
        }
        return sb.toString();
    }

    static String buildTitle(String rawModel, String customerName) {
        String model = rawModel == null || rawModel.isBlank() ? "未指明型号" : rawModel.trim();
        String title = model + " - " + customerName.trim();
        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    /** spec 页38 ⑤：审计只落 IP/UA 的哈希（SHA-256 前 16 位），不落原始值。 */
    static String sha256Short(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String deriveListingStatus(Product p) {
        if (p.getModelCode() != null && !p.getModelCode().isBlank()) {
            return "ON_SALE";
        }
        if (p.getProjectId() != null) {
            return "IN_DEV";
        }
        return "OTHER";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void audit(String action, Long entityId, String ipHash, String uaHash, String detail) {
        auditLogService.append(AuditLog.builder()
            .operatorName("GUEST").operatorRole("GUEST")
            .action(action).entityType("guest_demand").entityId(entityId)
            .ipAddress(ipHash)
            .afterData(AuditEventData.json("detail",
                "ipHash=" + ipHash + ";uaHash=" + uaHash + ";" + detail))
            .build());
    }

    /** 单机滑动窗口（1 小时窗口，条目懒过期清理）；多实例部署替换为 Redis 实现即可。 */
    static final class InMemoryHourRateLimiter implements GuestRateLimiter {
        private static final java.util.concurrent.ConcurrentHashMap<String, Window> WINDOWS =
            new java.util.concurrent.ConcurrentHashMap<>();
        static final class Window {
            final long start = System.currentTimeMillis();
            int count;
        }

        @Override
        public synchronized boolean tryAcquire(String ipHash) {
            long now = System.currentTimeMillis();
            WINDOWS.entrySet().removeIf(e -> now - e.getValue().start >= 3_600_000L);
            Window w = WINDOWS.computeIfAbsent(ipHash, k -> new Window());
            if (now - w.start >= 3_600_000L) {
                WINDOWS.put(ipHash, new Window());
                w = WINDOWS.get(ipHash);
            }
            w.count++;
            return w.count <= RATE_LIMIT_PER_HOUR;
        }
    }
}
