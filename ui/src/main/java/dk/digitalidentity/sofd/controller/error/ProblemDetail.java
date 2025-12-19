// Emulate RFC 7807 ProblemDetail for forward compatibility with Spring Boot 4
// Remove this class and use Spring ProblemDetail after updating to Spring Boot 4
package dk.digitalidentity.sofd.controller.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
    private String type = "about:blank";
    private String title;
    private Integer status;
    private String detail;
    private String instance;

    // For additional properties like validation errors
    private Map<String, Object> properties;

    public static ProblemDetail forStatusAndDetail(int status, String detail) {
        ProblemDetail problemDetail = new ProblemDetail();
        problemDetail.setStatus(status);
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
}