package dk.digitalidentity.sofd.controller.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorDTO {
	private String errorCode;
	private String errorMessage;
	
	public ErrorDTO(String code, String message) {
		this.errorCode = code;
		this.errorMessage = message;
	}
}
