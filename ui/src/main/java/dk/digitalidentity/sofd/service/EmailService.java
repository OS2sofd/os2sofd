package dk.digitalidentity.sofd.service;

import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.PreencodedMimeBodyPart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.InlineImageDTO;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	public boolean sendMessage(String email, String subject, String message, List<Attachment> attachments, List<InlineImageDTO> inlineImages, EmailTemplateChild templateChild) {
		if (!configuration.getIntegrations().getEmail().isEnabled()) {
			log.warn("email server is not configured - not sending email to " + email);
			return false;
		}

		Transport transport = null;

		log.info("Sending email: '" + subject + "' to " + email);

		try {
			Properties props = System.getProperties();
			props.put("mail.transport.protocol", "smtps");
			props.put("mail.smtp.port", 25);
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
			Session session = Session.getDefaultInstance(props);

			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(configuration.getIntegrations().getEmail().getFrom(), configuration.getIntegrations().getEmail().getFromName()));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

			if (templateChild != null) {
				if (templateChild.getRecipientsCC() != null) {
					List<String> recipients = emailTemplateChildService.getRecipientsList(templateChild.getRecipientsCC());
					InternetAddress[] cc = emailTemplateChildService.convertRecipientsToAddresses(recipients);
					msg.setRecipients(Message.RecipientType.CC, cc);
				}

				if (templateChild.getRecipientsBCC() != null) {
					List<String> recipients = emailTemplateChildService.getRecipientsList(templateChild.getRecipientsBCC());
					InternetAddress[] bcc = emailTemplateChildService.convertRecipientsToAddresses(recipients);
					msg.setRecipients(Message.RecipientType.BCC, bcc);
				}
			}

			msg.setSubject(subject, "UTF-8");

			MimeBodyPart htmlBodyPart = new MimeBodyPart();
			htmlBodyPart.setContent(message, "text/html; charset=UTF-8");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(htmlBodyPart);

			if (attachments != null) {
				for (Attachment attachment : attachments) {
					MimeBodyPart attachmentBodyPart = new MimeBodyPart();
					attachmentBodyPart = new MimeBodyPart();
					DataSource source = new ByteArrayDataSource(attachment.getFile().getContent(), "application/octet-stream");
					attachmentBodyPart.setDataHandler(new DataHandler(source));
					attachmentBodyPart.setFileName(attachment.getFilename());
					
					multipart.addBodyPart(attachmentBodyPart);
				}
			}
			
			// adds inline image attachments
	        if (inlineImages != null && inlineImages.size() > 0) {
	             
	            for (InlineImageDTO inlineImageDTO : inlineImages) {
	                
	                if (inlineImageDTO.isBase64()) {
	                	MimeBodyPart imagePart = new PreencodedMimeBodyPart("base64");
	                	String src = inlineImageDTO.getSrc();
	                	String dataType = StringUtils.substringBetween(src, "data:", ";base64,"); // extract data type ( fx dataType = "image/png") 
	                	String base64EncodedFileContent = src.replaceFirst("data:.*;base64,", ""); // remove prefix from fileContent String ( fx base64EncodedFileContent = "iVBORw0KGg......etc"
	                	imagePart.setContent(base64EncodedFileContent, dataType);
	                	imagePart.setFileName(inlineImageDTO.getCid());
	                	imagePart.setHeader("Content-ID", "<" + inlineImageDTO.getCid() + ">");
	                	imagePart.setDisposition(MimeBodyPart.INLINE);
	                	imagePart.setDisposition(Part.ATTACHMENT);
	                	multipart.addBodyPart(imagePart);
	                }
	            }
	        }

			msg.setContent(multipart);

			transport = session.getTransport();
			transport.connect(configuration.getIntegrations().getEmail().getHost(), configuration.getIntegrations().getEmail().getUsername(), configuration.getIntegrations().getEmail().getPassword());
			transport.addTransportListener(new TransportErrorHandler());
			transport.sendMessage(msg, msg.getAllRecipients());
		}
		catch (Exception ex) {
			log.error("Failed to send email to '" + email + "'", ex);
			
			return false;
		}
		finally {
			try {
				if (transport != null) {
					transport.close();
				}
			}
			catch (Exception ex) {
				log.warn("Error occured while trying to terminate connection", ex);
			}
		}
		
		return true;
	}
}
