package org.ruoyi.ipd.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.dto.GuestDemandSubmitReq;
import org.ruoyi.ipd.dto.GuestDemandSubmittedView;
import org.ruoyi.ipd.dto.PublicProductView;
import org.ruoyi.ipd.service.GuestDemandService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P4-1.1 需求门户公开端点（免登录；由 IpdWebSecurityConfig 放行 /api/v1/public/**）。
 * <ul>
 *   <li>POST /api/v1/public/demands——游客提交，返回 8 位查询码（页38）。</li>
 *   <li>GET /api/v1/public/products——三情形选择源（在售/在研/其他），仅返回 ACTIVE 产品。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicPortalController {

    private final GuestDemandService guestDemandService;

    public PublicPortalController(GuestDemandService guestDemandService) {
        this.guestDemandService = guestDemandService;
    }

    @PostMapping("/demands")
    public ApiV1Response<GuestDemandSubmittedView> submit(@RequestBody GuestDemandSubmitReq req,
                                                          HttpServletRequest http) {
        return ApiV1Response.ok(guestDemandService.submit(req, clientIp(http), http.getHeader("User-Agent")));
    }

    @GetMapping("/products")
    public ApiV1Response<List<PublicProductView>> products() {
        return ApiV1Response.ok(guestDemandService.publicProducts());
    }

    /**
     * SEC-REV-05：客户端 IP 仅取 servlet 远端地址，不信任 X-Forwarded-For。
     *
     * <p>原因：公开端点位于 IPD 单企业私有部署，前面没有反向代理。直接信任 XFF
     * 会被任意客户端伪造以绕过限流（10 次/小时）。若日后挂上反向代理，应通过网关
     * 设置专用 token 头或 mTLS 标识信任，再单独接入白名单头。当前仅信任 servlet
     * 远端地址，与 Tomcat/Undertow 的 access_log 字段一致。
     */
    private static String clientIp(HttpServletRequest http) {
        return http.getRemoteAddr();
    }
}
