package org.ruoyi.ipd.qa;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-04 验收线 1 的代码侧契约测试（DDL 侧对照由
 * docs/ipd-系统说明/验收/qa04_ddl_entity_mapping.py 完成，两者互为交叉验证）。
 *
 * 覆盖卡面要点：JSON 列 ↔ String 契约、datetime 列 ↔ Date、
 * del_flag 存在性（含 stage_actions/gate_review_elements 现状分歧的固化说明，见 QA-04 报告 DEF-04）、
 * audit_logs 只追加表不继承 BaseEntity 的设计约束。
 */
@Tag("dev")
class Qa04EntityContractTest {

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static String tableName(Class<?> type) {
        TableName tn = type.getAnnotation(TableName.class);
        assertThat(tn).as("%s 必须声明 @TableName", type.getSimpleName()).isNotNull();
        return tn.value();
    }

    @Test
    void coreEntitiesMapToExpectedTables() {
        assertThat(tableName(Project.class)).isEqualTo("projects");
        assertThat(tableName(StageAction.class)).isEqualTo("stage_actions");
        assertThat(tableName(Deliverable.class)).isEqualTo("deliverables");
        assertThat(tableName(GateReview.class)).isEqualTo("gate_reviews");
        assertThat(tableName(GateElement.class)).isEqualTo("gate_review_elements");
        assertThat(tableName(BonusPool.class)).isEqualTo("bonus_pools");
        assertThat(tableName(KpiRecord.class)).isEqualTo("kpi_records");
        assertThat(tableName(AuditLog.class)).isEqualTo("audit_logs");
    }

    @Test
    void jsonColumnsBindToStringPerTypeMappingContract() throws Exception {
        // type-mapping.md §1.4：JSONB/JSON → MySQL 原生 JSON，实体侧 String + JSON 契约
        assertThat(field(Project.class, "targetMarkets").getType()).isEqualTo(String.class);
        assertThat(field(BonusPool.class, "distributions").getType()).isEqualTo(String.class);
        assertThat(field(KpiRecord.class, "sharedDetail").getType()).isEqualTo(String.class);
        assertThat(field(AuditLog.class, "afterData").getType()).isEqualTo(String.class);
        assertThat(field(AuditLog.class, "beforeData").getType()).isEqualTo(String.class);
    }

    @Test
    void datetimeColumnsBindToUtilDate() throws Exception {
        assertThat(field(Project.class, "launchDate").getType()).isEqualTo(Date.class);
        assertThat(field(GateReview.class, "signedAt").getType()).isEqualTo(Date.class);
        assertThat(field(GateReview.class, "dueAt").getType()).isEqualTo(Date.class);
        assertThat(field(StageAction.class, "actualDoneAt").getType()).isEqualTo(Date.class);
        assertThat(field(Project.class, "createTime").getType()).isEqualTo(Date.class);
    }

    @Test
    void stageActionVersionIsIntegerAndAnnotated() throws Exception {
        Field version = field(StageAction.class, "version");
        assertThat(version.getType()).isEqualTo(Integer.class);
        assertThat(version.getAnnotation(Version.class))
            .as("stage_actions 乐观锁必须标 @Version（P1-4.3）").isNotNull();
        TableField tf = version.getAnnotation(TableField.class);
        assertThat(tf).isNotNull();
        assertThat(tf.insertStrategy()).isEqualTo(com.baomidou.mybatisplus.annotation.FieldStrategy.NOT_NULL);
    }

    @Test
    void auditLogIsAppendOnlyAndNotBaseEntity() {
        // AC-AUD-01：只追加表——不继承 BaseEntity（无 update_by/update_time/del_flag 语义）
        assertThat(BaseEntity.class.isAssignableFrom(AuditLog.class)).isFalse();
    }

    @Test
    void softDeletableEntitiesCarryDelFlag() throws Exception {
        assertThat(field(Project.class, "delFlag").getType()).isEqualTo(String.class);
        assertThat(field(Deliverable.class, "delFlag").getType()).isEqualTo(String.class);
        // 现状分歧（QA-04 报告 DEF-04）：stage_actions / gate_review_elements 的 DDL 有 del_flag，
        // 实体刻意不映射（当前无软删入口）。此处仅固化存在性事实，不做通过/失败判定。
        assertThat(hasField(StageAction.class, "delFlag")).isFalse();
        assertThat(hasField(GateElement.class, "delFlag")).isFalse();
    }

    private static boolean hasField(Class<?> type, String name) {
        try {
            field(type, name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
