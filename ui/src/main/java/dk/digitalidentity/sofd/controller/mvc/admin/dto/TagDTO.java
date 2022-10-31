package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import dk.digitalidentity.sofd.dao.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {
	private long id;
	private String value;
	private String description;
	private boolean customValueEnabled;
	private boolean customValueUnique;
	private String customValueName;
	private String customValueRegex;
	private boolean selected;
	private String customValue;
	
	public TagDTO(Tag t) {
		this.id = t.getId();
		this.description = t.getDescription();
		this.value = t.getValue();
		this.customValueEnabled = t.isCustomValueEnabled();
		this.customValueUnique = t.isCustomValueUnique();
		this.customValueName = t.getCustomValueName();
		this.customValueRegex = t.getCustomValueRegex();
		this.selected = false;
		this.customValue = "";
	}
}
