package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionTypeDTO {
    private long id;
    private String name;
    private List<PhoneType> phoneTypes;
    
    public FunctionTypeDTO(FunctionType functionType) {
    	this.id = functionType.getId();
    	this.name = functionType.getName();
    	this.phoneTypes = new ArrayList<>();
    	
    	if (functionType.getPhoneTypes() != null) {
    		this.phoneTypes = functionType.getPhoneTypes().stream().map(p -> p.getPhoneType()).collect(Collectors.toList());
    	}
    }
}
