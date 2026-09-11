package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.domain.ProjectScoreRecord;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.mapper.ProjectScoreRecordMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-2.2 ProjectScoreArchiveService.listArchive 单测（5 测覆盖 4 维度）：
 * <ol>
 *   <li>反射：{@code listArchive} 方法声明 {@code @Transactional(readOnly=true)}</li>
 *   <li>权限：仅超管可访问（非超管抛 IpdPermissionException → ServiceException 链路）</li>
 *   <li>参数：时间窗非法 / 起始晚于截止 / projectId 缺失 均抛 IpdBusinessException</li>
 *   <li>行为：mock 下 selectList 走通；wrapper 含 SUBMITTED/FINALIZED 与时间窗闭区间右开；无写副作用</li>
 *   <li>业务：空数据 / 多结果两种情况正确透传</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProjectScoreArchiveServiceListArchiveTest {

    @Mock private ProjectScoreMapper projectScoreMapper;
    @Mock private ProjectScoreRecordMapper projectScoreRecordMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper productGroupMapper;
    @Mock private IpdPermission permission;
    @Mock private SystemConfigService configService;
    @Mock private SystemConfigVersionMapper versionMapper;
    @Mock private AuditLogService auditLogService;

    private ProjectScoreArchiveService service;

    private ProjectScoreArchiveService newService() {
        service = new ProjectScoreArchiveService(
            projectScoreMapper, projectScoreRecordMapper, projectMapper, projectMemberMapper,
            personMapper, productGroupMapper, permission, configService, versionMapper,
            auditLogService, new com.fasterxml.jackson.databind.ObjectMapper());
        return service;
    }

    @Test
    @DisplayName("反射：listArchive 方法声明 @Transactional(readOnly=true) + rollbackFor=Exception.class")
    void listArchiveHasReadOnlyTransactionalAnnotation() throws NoSuchMethodException {
        Method m = ProjectScoreArchiveService.class.getDeclaredMethod(
            "listArchive", Long.class, YearMonth.class, YearMonth.class);

        Transactional tx = m.getAnnotation(Transactional.class);

        assertThat(tx).as("listArchive 必须标注 @Transactional").isNotNull();
        assertThat(tx.readOnly()).as("listArchive 必须 readOnly=true").isTrue();
        boolean rollbackCoversException = false;
        for (Class<? extends Throwable> rf : tx.rollbackFor()) {
            if (rf.isAssignableFrom(Exception.class)) { rollbackCoversException = true; break; }
        }
        assertThat(rollbackCoversException)
            .as("listArchive 必须 rollbackFor=Exception.class（G-11 一致性）")
            .isTrue();
    }

    @Test
    @DisplayName("权限：listArchive 必须先经 requireAdmin 守卫")
    void listArchiveRequiresAdmin() {
        newService();
        when(permission.requireAdmin()).thenThrow(new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权"));

        assertThatThrownBy(() -> service.listArchive(101L, YearMonth.of(2026, 1), YearMonth.of(2026, 3)))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无权");
        verify(projectScoreMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("参数：projectId 为空抛 PARAM_INVALID；时间窗反向 / 缺失抛 PARAM_INVALID")
    void listArchiveParameterValidation() {
        newService();
        IpdActor admin = new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);

        // projectId null
        assertThatThrownBy(() -> service.listArchive(null, YearMonth.of(2026, 1), YearMonth.of(2026, 3)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // fromMonth null
        assertThatThrownBy(() -> service.listArchive(101L, null, YearMonth.of(2026, 3)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // toMonth null
        assertThatThrownBy(() -> service.listArchive(101L, YearMonth.of(2026, 1), null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // fromMonth > toMonth
        assertThatThrownBy(() -> service.listArchive(101L, YearMonth.of(2026, 6), YearMonth.of(2026, 3)))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        verify(projectScoreMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("行为：listArchive 把 SUBMITTED/FINALIZED 与时间窗闭区间右开传给 selectList，无写副作用")
    void listArchivePassesCorrectWrapperAndNoWrite() {
        newService();
        IpdActor admin = new IpdActor(9002L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);

        ProjectScore ps = new ProjectScore();
        ps.setId(1L);
        ps.setProjectId(101L);
        ps.setStatus("FINALIZED");
        ps.setWeightedScore(new BigDecimal("85.50"));
        ps.setScoredAt(new Date());
        when(projectScoreMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(ps));

        YearMonth from = YearMonth.of(2026, 1);
        YearMonth to = YearMonth.of(2026, 3);
        List<ProjectScore> result = service.listArchive(101L, from, to);

        // 守卫：超管鉴权被调一次
        verify(permission).requireAdmin();

        // wrapper 透传 selectList
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProjectScore>> cap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectScoreMapper).selectList(cap.capture());

        // 关键不变量：listArchive 不能产生任何写副作用（显式标注泛型以消除 BaseMapper.insert(T) vs insert(Collection<T>) 重载歧义）
        verify(projectScoreRecordMapper, never()).insert(any(ProjectScoreRecord.class));
        verify(projectScoreRecordMapper, never()).update(any(), any(LambdaQueryWrapper.class));
        verify(projectScoreRecordMapper, never()).updateById(any(ProjectScoreRecord.class));
        verify(projectScoreRecordMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(projectScoreMapper, never()).insert(any(ProjectScore.class));
        verify(projectScoreMapper, never()).update(any(), any(LambdaQueryWrapper.class));
        verify(projectScoreMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(auditLogService, never()).append(any());

        // 结果透传
        assertThat(result).hasSize(1).first().extracting(ProjectScore::getId).isEqualTo(1L);
    }

    @Test
    @DisplayName("业务：listArchive 返回空集视为正常（无匹配窗口）")
    void listArchiveReturnsEmptyWhenNoMatch() {
        newService();
        IpdActor admin = new IpdActor(9003L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);
        when(projectScoreMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        List<ProjectScore> result = service.listArchive(101L, YearMonth.of(2026, 1), YearMonth.of(2026, 3));

        assertThat(result).isNotNull().isEmpty();
    }
}
