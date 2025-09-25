package dk.digitalidentity.sofd.controller.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.mvc.dto.ClientDTO;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntityField;
import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.service.ClientService;

@Component
public class ClientDTOValidator implements Validator {

	@Autowired
	private ClientService clientService;

	@Override
	public boolean supports(Class<?> aClass) {
		return (ClientDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		ClientDTO clientDTO = (ClientDTO) o;

		if (clientDTO.getId() == 0) {
			Client client = clientService.getClientByName(clientDTO.getName());
			if (client != null) {
				errors.rejectValue("name", "mvc.errors.client.exists");
			}
			
			client = clientService.getClientByApiKeyBypassCache(clientDTO.getApiKey());
			if (client != null) {
				errors.rejectValue("apiKey", "mvc.errors.client.apiKey.invalid");
			}
		}

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "mvc.errors.client.name.required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "apiKey", "mvc.errors.client.apiKey.required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "accessRole", "mvc.errors.client.accessRole.required");

		if (clientDTO.getName().length() < 3) {
			errors.rejectValue("name", "mvc.errors.client.name.length");
		}

		if (clientDTO.getApiKey().length() < 10 || clientDTO.getApiKey().length() > 36) {
			errors.rejectValue("apiKey", "mvc.errors.client.apiKey.length");
		}

		if (!Arrays.asList(AccessRole.values()).stream().anyMatch(ar -> ar.toString().equals(clientDTO.getAccessRole()))) {
			errors.rejectValue("accessRole", "mvc.errors.client.accessRole.value");
		}

		AccessRole accessRole = AccessRole.valueOf(clientDTO.getAccessRole());
		if (accessRole.equals(AccessRole.LIMITED_READ_ACCESS)) {
			String[] selectedFields = clientDTO.getFieldList().split(";");
			ArrayList<String> reducedList = new ArrayList<String>(Arrays.asList(selectedFields));
			reducedList.removeAll(Arrays.asList(AccessEntityField.values()).stream().map(aef -> aef.toString()).collect(Collectors.toList()));

			if (!reducedList.isEmpty()) {
				errors.rejectValue("accessRole", "mvc.errors.client.fieldList.value");
			}
		}
	}
}