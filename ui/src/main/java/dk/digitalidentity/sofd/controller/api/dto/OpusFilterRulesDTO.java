package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpusFilterRulesDTO {
	private boolean enabled;
	private List<String> positionIds;
	private List<String> positionNames;
	private List<String> losIds;
	private String orgUnitInfix;
	private List<String> invalidPositionNames;
}
