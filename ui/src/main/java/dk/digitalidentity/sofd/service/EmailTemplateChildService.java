package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.EmailTemplateChildDao;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailTemplateChildService {

	@Autowired
	private EmailTemplateChildDao emailTemplateChildDao;

	public EmailTemplateChild save(EmailTemplateChild templateChild) {
		return emailTemplateChildDao.save(templateChild);
	}
	
	public EmailTemplateChild findById(long id) {
		return emailTemplateChildDao.findById(id);
	}
	
	public void active(EmailTemplateChild templateChild) {
		templateChild.setEnabled(true);
		save(templateChild);
	}
	
	
	public void deactive(EmailTemplateChild templateChild) {
		templateChild.setEnabled(false);
		save(templateChild);
	}
	
	public void delete(EmailTemplateChild templateChild) {
		emailTemplateChildDao.delete(templateChild);
	}
	
	public List<EmailTemplateChild> findAll() {
		return (List<EmailTemplateChild>) emailTemplateChildDao.findAll();
	}
	
	public List<String> getRecipientsList(String templateRecipients) {
		List<String> recipients = new ArrayList<String>();
		if( templateRecipients != null )
		{
			for (String recipient : templateRecipients.split(";")) {
				if(StringUtils.hasLength(recipient)) {
					recipients.add(recipient.trim());
				}
			}
		}

		return recipients;
	}

	public InternetAddress[] convertRecipientsToAddresses(List<String> recipients) {
		List<InternetAddress> result = new ArrayList<>();

		for (String recipient : recipients) {
			try {
				if (!recipient.trim().isEmpty()) {
					result.add(new InternetAddress(recipient));
				}
			}
			catch (AddressException e) {
				log.warn("Unable to parse CC/BCC recipient: " + recipient);
			}
		}

		return result.stream().toArray(InternetAddress[]::new);
	}
}
