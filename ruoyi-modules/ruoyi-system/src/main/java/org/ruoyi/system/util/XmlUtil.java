package org.ruoyi.system.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author https://www.wdbyte.com
 */
@Slf4j
public class XmlUtil {

    public static String xml2json(String requestBody) {
        requestBody = StringUtils.trim(requestBody);
        XmlMapper xmlMapper = new XmlMapper();
        JsonNode node = null;
        try {
            node = xmlMapper.readTree(requestBody.getBytes());
            ObjectMapper jsonMapper = new ObjectMapper();
            return jsonMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.error("xml 2 json error,msg:" + e.getMessage(), e);
        }
        return null;
    }
}
