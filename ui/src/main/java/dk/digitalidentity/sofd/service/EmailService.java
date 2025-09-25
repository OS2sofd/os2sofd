package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Base64;
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

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentCollectionResponse;
import com.microsoft.graph.requests.GraphServiceClient;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.InlineImageDTO;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static org.springframework.util.StringUtils.hasLength;

@Slf4j
@Service
public class EmailService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	public boolean sendMessage(String emailTo, String subject, String message, List<Attachment> attachments, List<InlineImageDTO> inlineImages, EmailTemplateChild templateChild, String logContext) {
		if (!configuration.getIntegrations().getEmail().isEnabled()) {
			log.warn("email server is not configured - not sending email to " + emailTo);
			return false;
		}

		return switch (configuration.getIntegrations().getEmail().getClientType()) {
			case SMTP -> sendMessageSMTP(emailTo, subject, message, attachments, inlineImages, templateChild, logContext);
			case GRAPH -> sendMessageGraph(emailTo, subject, message, attachments, inlineImages, templateChild, logContext);
            default -> {
                log.error("Unknown Email ClientType");
				yield false;
            }
		};
	}

	public boolean sendMessageSMTP(String emailTo, String subject, String message, List<Attachment> attachments, List<InlineImageDTO> inlineImages, EmailTemplateChild templateChild, String logContext) {
		Transport transport = null;

		log.info("Sending email: '" + subject + "' to " + emailTo + " using smtp");

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
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(emailTo));

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
			
			auditLogSend(msg.getSubject(), templateChild, emailTo, logContext);
		}
		catch (Exception ex) {
			log.error("Failed to send email to '" + emailTo + "'", ex);
			
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

	public boolean sendMessageGraph(String emailTo, String subject, String message, List<Attachment> attachments, List<InlineImageDTO> inlineImages, EmailTemplateChild templateChild, String logContext) {
		try {
			log.info("Sending email: '" + subject + "' to " + emailTo + " using graph");

			var graphClient = initializeGraphAuth();
			var request = new UserSendMailParameterSet();

			request.message = new com.microsoft.graph.models.Message();
			request.message.subject = subject;
			request.message.toRecipients = List.of(toRecipient(emailTo));

			if (templateChild != null) {
				if (templateChild.getRecipientsCC() != null) {
					List<String> recipients = emailTemplateChildService.getRecipientsList(templateChild.getRecipientsCC());
					request.message.ccRecipients = recipients.stream().map(this::toRecipient).toList();
				}

				if (templateChild.getRecipientsBCC() != null) {
					List<String> recipients = emailTemplateChildService.getRecipientsList(templateChild.getRecipientsBCC());
					request.message.bccRecipients = recipients.stream().map(this::toRecipient).toList();
				}
			}

			request.message.body = new ItemBody();
			request.message.body.content = message;
			request.message.body.contentType = BodyType.HTML;

			var fileAttachments = new ArrayList<com.microsoft.graph.models.Attachment>();
			if (attachments != null) {
				for( var attachment : attachments ) {
					var fileAttachment = new FileAttachment();
					fileAttachment.name = attachment.getFilename();
					fileAttachment.contentBytes = attachment.getFile().getContent();
					fileAttachment.oDataType = "#microsoft.graph.fileAttachment";
					fileAttachments.add(fileAttachment);
				}
			}

			if (inlineImages != null) {
				for( var inlineImage : inlineImages ) {
					var inlineAttachment = new FileAttachment();
					inlineAttachment.name = inlineImage.getCid();
					inlineAttachment.contentType = StringUtils.substringBetween(inlineImage.getSrc(), "data:", ";base64,"); // extract data type ( fx dataType = "image/png")
					inlineAttachment.contentId = inlineImage.getCid();
					var base64EncodedFileContent = inlineImage.getSrc().replaceFirst("data:.*;base64,", ""); // remove prefix from fileContent String ( fx base64EncodedFileContent = "iVBORw0KGg......etc"
					inlineAttachment.contentBytes = Base64.getDecoder().decode(base64EncodedFileContent);
					inlineAttachment.oDataType = "#microsoft.graph.fileAttachment";
					fileAttachments.add(inlineAttachment);
				}
			}

			if (!fileAttachments.isEmpty()) {
				AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
				attachmentCollectionResponse.value = fileAttachments;
				request.message.attachments = new AttachmentCollectionPage(attachmentCollectionResponse, null);
				request.message.hasAttachments = true;
			}

			graphClient.users(configuration.getIntegrations().getEmail().getFrom()).sendMail(request).buildRequest().post();
			
			auditLogSend(subject, templateChild, emailTo, logContext);
		}
		catch (Exception ex) {
			log.warn("Failed to send email to '" + emailTo + "'", ex);
			return false;
		}
		
		return true;
	}

	private void auditLogSend(String subject, EmailTemplateChild templateChild, String email, String logContext) {
		StringBuilder logMessage = new StringBuilder();
		logMessage.append("Email sendt til " + email + " med emnet: " + subject);
		
		if (templateChild != null) {
			if (templateChild.getRecipientsCC() != null) {
				logMessage.append(", CC: " + templateChild.getRecipientsCC());
			}

			if (templateChild.getRecipientsBCC() != null) {
				logMessage.append(", BCC: " + templateChild.getRecipientsBCC());
			}
		}
		if(hasLength(logContext) ) {
			logMessage.append(", Kontekst: " + logContext);
		}
		auditLogger.log(null, EntityType.EMAIL, EventType.MESSAGE_SEND, email, logMessage.toString());
	}

	private Recipient toRecipient(String email) {
		var emailAddress = new EmailAddress();
		emailAddress.address = email;
		
		var recipient = new Recipient();
		recipient.emailAddress = emailAddress;
		
		return recipient;
	}

	private GraphServiceClient<Request> initializeGraphAuth() throws ClientException {
		// Create the auth provider
		final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
				.tenantId(configuration.getIntegrations().getEmail().getTenantId())
				.clientId(configuration.getIntegrations().getEmail().getClientId())
				.clientSecret(configuration.getIntegrations().getEmail().getSecret())
				.build();

		TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(credential);

		DefaultLogger defaultLogger = new DefaultLogger();

		// Build a Graph client
		return GraphServiceClient.builder()
				.authenticationProvider(authProvider)
				.logger(defaultLogger)
				.buildClient();
	}
}
