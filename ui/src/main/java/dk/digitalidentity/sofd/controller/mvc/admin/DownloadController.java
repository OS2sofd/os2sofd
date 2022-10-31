package dk.digitalidentity.sofd.controller.mvc.admin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.EmailTemplateDao;
import dk.digitalidentity.sofd.dao.SettingDao;
import dk.digitalidentity.sofd.dao.SupportedUserTypeDao;
import dk.digitalidentity.sofd.dao.model.ClientConfig;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.ClientConfigService;

@RequireAdminAccess
@Controller
public class DownloadController {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private SettingDao settingDao;
	
	@Autowired
	private EmailTemplateDao emailTemplateDao;
	
	@Autowired
	private SupportedUserTypeDao supportedUserTypeDao;

	@Autowired
	private ClientConfigService clientConfigService;

	@GetMapping(value = "/ui/admin/downloadconfig")
	public void downloadConfiguration(HttpServletResponse response) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		
		// serialize SofdConfiguration
		String sofdConfigurationJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration);

		// serialize Settings table
		List<Setting> settings = settingDao.findAll();
		String settingsTableJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);

		// serialize EmailTemplate table
		List<EmailTemplate> emails = emailTemplateDao.findAll();
		String emailTemplatesTableJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(emails);

		// serialize SupportedUserTypes table
		List<SupportedUserType> supportedUserTypes = supportedUserTypeDao.findAll();
		String supportedUserTypesTableJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(supportedUserTypes);

		// Generate ZIP file
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream out = new ZipOutputStream(bos);

		ZipEntry e = new ZipEntry("SofdConfiguration.json");
		out.putNextEntry(e);
		out.write(sofdConfigurationJson.getBytes(Charset.forName("UTF-8")));
		out.closeEntry();

		e = new ZipEntry("SettingsTable.json");
		out.putNextEntry(e);
		out.write(settingsTableJson.getBytes(Charset.forName("UTF-8")));
		out.closeEntry();
		
		e = new ZipEntry("EmailTemplatesTable.json");
		out.putNextEntry(e);
		out.write(emailTemplatesTableJson.getBytes(Charset.forName("UTF-8")));
		out.closeEntry();
		
		e = new ZipEntry("SupportedUserTypesTable.json");
		out.putNextEntry(e);
		out.write(supportedUserTypesTableJson.getBytes(Charset.forName("UTF-8")));
		out.closeEntry();

		// append all client configs
		List<ClientConfig> clientConfigs = clientConfigService.getAll();
		for (ClientConfig clientConfig : clientConfigs) {
			e = new ZipEntry(clientConfig.getClientId() + " - " + clientConfig.getClientName() + ".zip");
			out.putNextEntry(e);
			out.write(clientConfig.getConfiguration());
			out.closeEntry();
		}

		out.close();

		// send
		
		response.setContentType("application/pdf");
		response.addHeader("Content-Disposition", "attachment; filename=config.zip");

		response.getOutputStream().write(bos.toByteArray());
		response.getOutputStream().flush();
	}
}
