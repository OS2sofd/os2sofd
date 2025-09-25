package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectedNumbersDTO {
	private HashSet<String> selectedNumbers;
}
