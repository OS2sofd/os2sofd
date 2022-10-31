package dk.digitalidentity.sofd.service.eboks.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EboksMessage {
	private String cpr;
	private String cvr;
	private String subject;
	
	private List<EboksAttachment> attachments;

	// TODO: gamle værdier
	private String senderId;
	private String contentTypeId;
	private String pdfFileBase64;

	// TODO: nye værdier
	private String municipalityName;
	private String content;

}
