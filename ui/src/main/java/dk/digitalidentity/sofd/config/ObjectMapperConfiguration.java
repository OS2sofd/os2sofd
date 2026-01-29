package dk.digitalidentity.sofd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
public class ObjectMapperConfiguration {

	@Bean
	public ObjectMapper objectMapper() {
		return JsonMapper.builder()
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.build();

	}
}
