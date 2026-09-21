package org.ruoyi.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.json.config.JacksonConfig;
import org.ruoyi.system.domain.bo.SysTenantBo;
import org.ruoyi.workflow.config.BeanConfig;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class TenantRequestJacksonTest {

    private static final String TENANT_REQUEST = """
        {
          "contactPhone": "13700000000",
          "expireTime": "2027-09-11 00:00:00",
          "accountCount": -1,
          "companyName": "第一集",
          "contactUserName": "刘备",
          "username": "user",
          "password": "123456",
          "packageId": "2018611998196109314"
        }
        """;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonConfig.class, JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class))
        .withUserConfiguration(BeanConfig.class);

    @Test
    void readsTenantRequestWithApplicationDateFormat() {
        contextRunner.withPropertyValues("spring.jackson.date-format=yyyy-MM-dd HH:mm:ss").run(context -> {
            MappingJackson2HttpMessageConverter converter = context.getBean(MappingJackson2HttpMessageConverter.class);
            assertSame(context.getBean(ObjectMapper.class), converter.getObjectMapper());

            SysTenantBo tenant = readTenant(converter, TENANT_REQUEST);

            assertEquals(expirationDate(), tenant.getExpireTime());
            assertEquals(2018611998196109314L, tenant.getPackageId());
            assertEquals(-1L, tenant.getAccountCount());
            assertEquals("第一集", tenant.getCompanyName());
            assertEquals("user", tenant.getUsername());
        });
    }

    @Test
    void keepsRegisteredDateDeserializerWithoutDateFormatProperty() {
        contextRunner.run(context -> {
            SysTenantBo tenant = readTenant(context.getBean(MappingJackson2HttpMessageConverter.class), TENANT_REQUEST);

            assertEquals(expirationDate(), tenant.getExpireTime());
        });
    }

    @Test
    void honorsConfiguredDateSerializationFormat() {
        contextRunner.withPropertyValues("spring.jackson.date-format=yyyy/MM/dd HH:mm:ss").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);

            String json = mapper.writeValueAsString(new DateValue(expirationDate()));

            assertEquals("{\"expireTime\":\"2027/09/11 00:00:00\"}", json);
        });
    }

    @Test
    void preservesWorkflowJsonRoundTripAndNullOmission() {
        contextRunner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            WorkflowValue value = new WorkflowValue(2018611998196109314L,
                LocalDateTime.of(2027, 9, 11, 0, 0), Optional.of("workflow"), null);

            String json = mapper.writeValueAsString(value);
            JsonNode tree = mapper.readTree(json);

            assertTrue(tree.get("id").isTextual());
            assertEquals("2018611998196109314", tree.get("id").asText());
            assertEquals("2027-09-11 00:00:00", tree.get("createdAt").asText());
            assertEquals("workflow", tree.get("name").asText());
            assertFalse(tree.has("remark"));
            assertEquals(value, mapper.readValue(json, WorkflowValue.class));
        });
    }

    @Test
    void acceptsNullExpirationDate() {
        contextRunner.run(context -> {
            String request = TENANT_REQUEST.replace("\"2027-09-11 00:00:00\"", "null");
            SysTenantBo tenant = readTenant(context.getBean(MappingJackson2HttpMessageConverter.class), request);

            assertNull(tenant.getExpireTime());
        });
    }

    @Test
    void rejectsInvalidExpirationDate() {
        contextRunner.run(context -> {
            String request = TENANT_REQUEST.replace("2027-09-11 00:00:00", "invalid-date");
            MappingJackson2HttpMessageConverter converter = context.getBean(MappingJackson2HttpMessageConverter.class);

            assertThrows(HttpMessageNotReadableException.class, () -> readTenant(converter, request));
        });
    }

    private static SysTenantBo readTenant(MappingJackson2HttpMessageConverter converter, String json) throws Exception {
        MockHttpInputMessage input = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
        input.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return (SysTenantBo) converter.read(SysTenantBo.class, input);
    }

    private static Date expirationDate() {
        return Date.from(LocalDateTime.of(2027, 9, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    private record DateValue(Date expireTime) {
    }

    private record WorkflowValue(Long id, LocalDateTime createdAt, Optional<String> name, String remark) {
    }
}
