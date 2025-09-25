package dk.digitalidentity.sofd.service.sms.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SendMessageDTO {
	private String cvr;
	private String content;
	private Set<String> numbers;
}
