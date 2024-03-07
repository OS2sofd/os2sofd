package dk.digitalidentity.sofd.controller.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.FunctionDTO;

@Component
public class FunctionDTOValidator implements Validator {

	@Override
	public boolean supports(Class<?> aClass) {
		return (FunctionDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		FunctionDTO functionDTO = (FunctionDTO) o;
		
		if (!StringUtils.hasLength(functionDTO.getName()) || functionDTO.getName().length() > 255 ) {
			errors.rejectValue("name", "mvc.errors.funtion.name");
		}
		
		if (!StringUtils.hasLength(functionDTO.getCategory()) || functionDTO.getCategory().length() > 255 ) {
			errors.rejectValue("category", "mvc.errors.funtion.category");
		}
	}
}