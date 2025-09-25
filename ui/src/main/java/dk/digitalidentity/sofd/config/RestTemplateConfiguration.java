package dk.digitalidentity.sofd.config;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.SSLContext;

import dk.digitalidentity.sofd.interceptor.RequestResponseLoggingInterceptor;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	RequestResponseLoggingInterceptor responseLoggingInterceptor;

	@Bean(name = "defaultRestTemplate")
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean(name = "opusRestTemplate")
	public RestTemplate opusRestTemplate() throws Exception {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(30000)
				.setConnectTimeout(30000)
				.setSocketTimeout(60000)
				.setCookieSpec(CookieSpecs.STANDARD)
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
			
			client = HttpClients.custom()
						.setSSLContext(sslContext)
						.setDefaultRequestConfig(requestConfig)
						.build();			
		}
		else {
			client = HttpClients.custom()
						.setDefaultRequestConfig(requestConfig)
						.build();
		}

		BufferingClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return false;
			}
			
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				;
			}
		});

		return restTemplate;
	}
}
