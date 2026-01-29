package dk.digitalidentity.sofd.config;

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.interceptor.RequestResponseLoggingInterceptor;

@Configuration
public class RestTemplateConfiguration {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private RequestResponseLoggingInterceptor responseLoggingInterceptor;

	@Bean(name = "defaultRestTemplate")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	// TODO: Use RestClient
    @SuppressWarnings("deprecation")
	@Bean(name = "trustEverythingRestTemplate")
	public RestTemplate trustEverythingRestTemplate() throws Exception {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] _, String _) -> true;

		SSLContext sslContext = SSLContextBuilder.create()
            .loadTrustMaterial(acceptingTrustStrategy)
            .build();

		SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .build();
        
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        
        // Build the HTTP client
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

		BufferingClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

		return new RestTemplate(requestFactory);
	}
	
    @SuppressWarnings("deprecation")
	@Bean(name = "opusRestTemplate")
	public RestTemplate opusRestTemplate() throws Exception {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] _, String _) -> true;

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(30, TimeUnit.SECONDS)
				.setResponseTimeout(120, TimeUnit.SECONDS)
				.setCookieSpec("standard")
				.build();

		CloseableHttpClient client = null;
		if (configuration.getModules().getAccountCreation().isEnabled() && configuration.getModules().getAccountCreation().getOpusHandler().isEnabled()) {
			SSLContext sslContext = SSLContextBuilder.create()
	                .loadKeyMaterial(
	                		ResourceUtils.getFile(configuration.getModules().getAccountCreation().getOpusHandler().getKeystore()),
	                		configuration.getModules().getAccountCreation().getOpusHandler().getKeystorePwd().toCharArray(),
	                		configuration.getModules().getAccountCreation().getOpusHandler().getKeystorePwd().toCharArray())
	                .loadTrustMaterial(acceptingTrustStrategy)
	                .build();

	        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
	                .setSslContext(sslContext)
	                .build();
	        
	        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
	                .setSSLSocketFactory(sslSocketFactory)
	                .build();
	        
	        // Build the HTTP client
	        client = HttpClients.custom()
					.setDefaultRequestConfig(requestConfig)
	                .setConnectionManager(connectionManager)
	                .build();
		}
		else {
			client = HttpClients.custom()
					.setDefaultRequestConfig(requestConfig)
					.build();
		}

		BufferingClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.setInterceptors(Collections.singletonList(responseLoggingInterceptor));
		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return false;
			}
			
			@Override
			public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
				;
			}
		});

		return restTemplate;
	}
}
