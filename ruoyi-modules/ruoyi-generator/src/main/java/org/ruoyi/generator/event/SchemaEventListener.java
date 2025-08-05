package org.ruoyi.generator.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.generator.service.SchemaFieldService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 数据模型事件监听器
 *
 * @author ruoyi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaEventListener {

    private final SchemaFieldService schemaFieldService;

    /**
     * 监听数据模型添加事件，自动插入字段数据
     */
    @Async
    @EventListener
    public void handleSchemaAddedEvent(SchemaAddedEvent event) {
        try {
            Long schemaId = event.getSchemaId();
            String tableName = event.getTableName();
            log.info("开始为数据模型字段 {} 自动插入字段数据，表名: {}", schemaId, tableName);
            boolean success = schemaFieldService.batchInsertFieldsByTableName(schemaId, tableName);
            if (success) {
                log.info("数据模型字段 {} 字段数据插入成功", schemaId);
            } else {
                log.warn("数据模型字段 {} 字段数据插入失败", schemaId);
            }
        } catch (Exception e) {
            log.error("自动插入字段数据失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 监听数据模型添加事件，自动插入字段数据
     */
    @Async
    @EventListener
    public void handleSchemaDeletedEvent(SchemaDeletedEvent event) {
        try {
            Collection<Long> schemaIds = event.getSchemaIds();
            log.info("开始为数据模型字段 {} 自动删除字段数据", schemaIds);
            boolean success = schemaFieldService.deleteWithValidBySchemaIds(schemaIds, true);
            if (success) {
                log.info("数据模型字段 {} 字段数据删除成功", schemaIds);
            } else {
                log.warn("数据模型字段 {} 字段数据删除失败", schemaIds);
            }
        } catch (Exception e) {
            log.error("自动删除字段数据失败: {}", e.getMessage(), e);
        }
    }
}