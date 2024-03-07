package dk.digitalidentity.sofd.controller.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.FacetDTO;

@Component
public class FacetDTOValidator implements Validator {

	@Override
	public boolean supports(Class<?> aClass) {
		return (FacetDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		FacetDTO facetDTO = (FacetDTO) o;
		
		if (!StringUtils.hasLength(facetDTO.getName()) || facetDTO.getName().length() > 255 ) {
			errors.rejectValue("name", "mvc.errors.facet.name");
		}
		
		if (facetDTO.getType() == null) {
			errors.rejectValue("type", "mvc.errors.facet.type");
		}
	}
}