package dk.digitalidentity.sofd.controller.rest.admin.model;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentDTO {
	
	// for sending to UI from backend
	private long id;
	private String filename;

	// for uploading from UI to backend
	private long templateId;
	private MultipartFile file;
}
