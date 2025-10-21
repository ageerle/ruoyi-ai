package org.ruoyi.workflow.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.WfNodeInputConfig;
import org.ruoyi.workflow.workflow.def.WfNodeIO;
import org.ruoyi.workflow.workflow.def.WfNodeParamRef;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.ruoyi.workflow.workflow.WfNodeIODataUtil.INPUT_TYPE_TO_NODE_IO_DEF;

@Slf4j
@MappedJdbcTypes({JdbcType.JAVA_OBJECT})
@MappedTypes({WfNodeInputConfig.class})
public class NodeInputConfigTypeHandler extends BaseTypeHandler<WfNodeInputConfig> {

    public static WfNodeInputConfig fillNodeInputConfig(String jsonSource) {
        ObjectNode jsonNode = (ObjectNode) JsonUtil.toJsonNode(jsonSource);
        return createNodeInputConfig(jsonNode);
    }

    public static WfNodeInputConfig createNodeInputConfig(ObjectNode jsonNode) {
        List<WfNodeIO> userInputs = new ArrayList<>();
        WfNodeInputConfig result = new WfNodeInputConfig();
        result.setUserInputs(userInputs);
        result.setRefInputs(new ArrayList<>());
        if (null == jsonNode) {
            return result;
        }
        ArrayNode userInputsJson = jsonNode.withArray("user_inputs");
        ArrayNode refInputs = jsonNode.withArray("ref_inputs");
        if (!userInputsJson.isEmpty()) {
            for (JsonNode userInput : userInputsJson) {
                if (userInput instanceof ObjectNode objectNode) {
                    int type = objectNode.get("type").asInt();
                    Class<? extends WfNodeIO> nodeIOClass = INPUT_TYPE_TO_NODE_IO_DEF.get(WfIODataTypeEnum.getByValue(type));
                    WfNodeIO wfNodeIO = JsonUtil.fromJson(objectNode, nodeIOClass);
                    if (null != wfNodeIO) {
                        userInputs.add(wfNodeIO);
                    } else {
                        log.warn("用户输入格式不正确:{}", userInput);
                    }
                }
            }
        }
        if (!refInputs.isEmpty()) {
            List<WfNodeParamRef> list = JsonUtil.fromArrayNode(refInputs, WfNodeParamRef.class);
            if (CollectionUtils.isNotEmpty(list)) {
                result.setRefInputs(list);
            } else {
                log.warn("引用输入格式不正确:{}", refInputs);
            }
        }
        return result;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, WfNodeInputConfig parameter, JdbcType jdbcType) {
//        PGobject jsonObject = new PGobject();
//        jsonObject.setType("jsonb");
//        try {
//            jsonObject.setValue(JsonUtil.toJson(parameter));
//            ps.setObject(i, jsonObject);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public WfNodeInputConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonSource = rs.getString(columnName);
        if (jsonSource != null) {
            try {
                return fillNodeInputConfig(jsonSource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public WfNodeInputConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonSource = rs.getString(columnIndex);
        if (jsonSource != null) {
            return fillNodeInputConfig(jsonSource);
        }
        return null;
    }

    @Override
    public WfNodeInputConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonSource = cs.getString(columnIndex);
        if (jsonSource != null) {
            try {
                return fillNodeInputConfig(jsonSource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}