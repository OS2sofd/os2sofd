package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.TemplateDao;
import dk.digitalidentity.sofd.dao.model.Template;

@Service
public class TemplateService {
	private final String GSM_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅabcdefghijklmnopqrstuvwxyzæøåàèìòùÄÖÜäöüÉéÑñç¥ß,.-:;!?&%<=>'\"()*+/#@£$_¤§¿¡";
	private final String EXTENDED_GSM_CHARSET = "^{}\\[]~|";

	@Autowired
	private TemplateDao templateDao;

	public boolean isValidMessage(String message) {
		int length = 0;

		for (char c : message.toCharArray()) {
			if (GSM_CHARSET.indexOf(c) == -1) {
				if (EXTENDED_GSM_CHARSET.indexOf(c) == -1) {
					return false;
				}
				else {
					length += 2;
				}

				length += 1;
			}
		}

		if (length > 160) {
			return false;
		}

		return true;
	}

	public List<Template> findAll() {
		return templateDao.findAll();
	}

	public Template findById(long templateId) {
		return templateDao.findById(templateId).orElse(null);
	}

	public void delete(Template template) {
		templateDao.delete(template);
	}

	public Template save(Template template) {
		return templateDao.save(template);
	}
}
