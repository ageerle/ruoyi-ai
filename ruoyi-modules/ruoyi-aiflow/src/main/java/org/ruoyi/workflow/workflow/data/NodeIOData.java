package org.ruoyi.workflow.workflow.data;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点输入输出数据
 */
@Builder
@Data
public class NodeIOData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected String name;

    protected NodeIODataContent<?> content;

    public static NodeIOData createByText(String name, String title, String value) {
        NodeIODataTextContent datContent = new NodeIODataTextContent();
        datContent.setValue(value);
        datContent.setTitle(title);
        return NodeIOData.builder().name(name).content(datContent).build();
    }

    public static NodeIOData createByNumber(String name, String title, Double value) {
        NodeIODataNumberContent datContent = new NodeIODataNumberContent();
        datContent.setValue(value);
        datContent.setTitle(title);
        return NodeIOData.builder().name(name).content(datContent).build();
    }

    public static NodeIOData createByBool(String name, String title, Boolean value) {
        NodeIODataBoolContent datContent = new NodeIODataBoolContent();
        datContent.setValue(value);
        datContent.setTitle(title);
        return NodeIOData.builder().name(name).content(datContent).build();
    }

    public static NodeIOData createByFiles(String name, String title, List<String> value) {
        NodeIODataFilesContent datContent = new NodeIODataFilesContent();
        datContent.setValue(value);
        datContent.setTitle(title);
        return NodeIOData.builder().name(name).content(datContent).build();
    }

    public static NodeIOData createByOptions(String name, String title, Map<String, Object> value) {
        NodeIODataOptionsContent datContent = new NodeIODataOptionsContent();
        datContent.setValue(value);
        datContent.setTitle(title);
        return NodeIOData.builder().name(name).content(datContent).build();
    }

    public String valueToString() {
        return content.getValue().toString();
    }
}
