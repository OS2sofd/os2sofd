package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChartDTO {
	private String name;
	private String manager;
	private String className;
	private List<ChartDTO> children;
	
	private boolean managerInherited;
	private String uuid;
}
