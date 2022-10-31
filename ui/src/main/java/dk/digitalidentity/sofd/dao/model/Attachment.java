package dk.digitalidentity.sofd.dao.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "email_templates_attachment")
@Getter
@Setter
public class Attachment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String filename;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "email_template_child_id")
	private EmailTemplateChild emailTemplateChild;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "file_id")
	private AttachmentFile file;
}
