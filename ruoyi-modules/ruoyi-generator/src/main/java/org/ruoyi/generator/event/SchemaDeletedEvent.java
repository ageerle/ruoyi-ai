package org.ruoyi.generator.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * 数据模型添加事件
 *
 * @author ruoyi
 */
@Getter
public class SchemaDeletedEvent extends ApplicationEvent {

    private final Collection<Long> schemaIds;

    public SchemaDeletedEvent(Object source, Collection<Long> schemaIds) {
        super(source);
        this.schemaIds = schemaIds;
    }
}