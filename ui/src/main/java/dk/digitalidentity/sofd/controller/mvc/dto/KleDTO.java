package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.Kle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KleDTO {
	private String id;
	private String parent;
	private String text;
	
	public KleDTO() {
		
	}

	public KleDTO(Kle kle) {
		this.setId(kle.getCode());
		this.setParent(kle.getParent().equals("0") ? "#" : kle.getParent());
		this.setText(kle.isActive() ? kle.getCode() + " " + kle.getName() : kle.getCode() + " " + kle.getName() + " [UDGÅET]");
	}
}
