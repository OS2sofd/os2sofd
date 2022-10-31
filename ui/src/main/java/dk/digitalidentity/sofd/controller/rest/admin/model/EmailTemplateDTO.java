package dk.digitalidentity.sofd.controller.rest.admin.model;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplateDTO {
	private long id;
	private String templateTypeText;
	private EmailTemplateType emailTemplateType;
	private List<EmailTemplateChildDTO> children;
}
