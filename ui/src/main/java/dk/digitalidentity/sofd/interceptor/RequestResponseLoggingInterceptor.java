package dk.digitalidentity.sofd.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@Slf4j
public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("===========================request begin================================================");
            log.trace("URI         : {}", request.getURI());
            log.trace("Method      : {}", request.getMethod());
            log.trace("Headers     : {}", request.getHeaders());
            log.trace("Request body: {}", new String(body, Charset.defaultCharset()));
            log.trace("==========================request end================================================");
        }
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("============================response begin==========================================");
            log.trace("Status code  : {}", response.getStatusCode());
            log.trace("Status text  : {}", response.getStatusText());
            log.trace("Headers      : {}", response.getHeaders());
            log.trace("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
            log.trace("=======================response end=================================================");
        }
    }

}
