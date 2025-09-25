package dk.digitalidentity.sofd.dao.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_templates")
public class EmailTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@Enumerated(EnumType.STRING)
	@NotNull
	private EmailTemplateType templateType;
	
	@OneToMany(mappedBy = "emailTemplate", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<EmailTemplateChild> children = new ArrayList<>();
}
