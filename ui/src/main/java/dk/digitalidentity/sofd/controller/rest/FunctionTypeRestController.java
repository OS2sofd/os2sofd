package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.dto.FunctionTypeDTO;
import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.mapping.FunctionTypePhoneTypeMapping;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequireDaoWriteAccess
@Slf4j
public class FunctionTypeRestController {

	@Autowired
	private FunctionTypeService functionTypeService;

	@PostMapping("/rest/functiontype/edit")
	@ResponseBody
	public ResponseEntity<String> editCategory(@RequestBody FunctionTypeDTO functionTypeDTO) {
		FunctionType functionType = new FunctionType();
		functionType.setPhoneTypes(new ArrayList<>());

		if (functionTypeDTO.getId() > 0) {
			functionType = functionTypeService.findById(functionTypeDTO.getId());

			if (functionType == null) {
				log.warn("Requested Function Type with ID:" + functionTypeDTO.getId() + " not found.");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		functionType.setName(functionTypeDTO.getName());
		
		Set<PhoneType> existingPhoneTypes = functionType.getPhoneTypes().stream().map(p -> p.getPhoneType()).collect(Collectors.toSet());
		
		// to add
		for (PhoneType phoneType : functionTypeDTO.getPhoneTypes()) {
			if (!existingPhoneTypes.contains(phoneType)) {
				FunctionTypePhoneTypeMapping mapping = new FunctionTypePhoneTypeMapping();
				mapping.setFunctionType(functionType);
				mapping.setPhoneType(phoneType);
				
				functionType.getPhoneTypes().add(mapping);
			}
		}
		
		// to remove
		for (Iterator<FunctionTypePhoneTypeMapping> iterator = functionType.getPhoneTypes().iterator(); iterator.hasNext();) {
			FunctionTypePhoneTypeMapping functionTypePhoneTypeMapping = iterator.next();
			
			if (!functionTypeDTO.getPhoneTypes().contains(functionTypePhoneTypeMapping.getPhoneType())) {
				iterator.remove();
			}
		}
		
		functionTypeService.save(functionType);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/rest/functiontype/delete")
	@ResponseBody
	public ResponseEntity<String> deleteCategory(@RequestBody FunctionTypeDTO functionTypeDTO) {
		FunctionType functionType = functionTypeService.findById(functionTypeDTO.getId());
		if (functionType == null) {
			log.warn("Requested Function Type with ID:" + functionTypeDTO.getId() + " not found.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		functionTypeService.delete(functionType);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
