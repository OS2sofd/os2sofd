package dk.digitalidentity.sofd.controller.mvc;

import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.ClientActivityDTO;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.ClientService;

@Controller
@PropertySource("classpath:git.properties")
public class DefaultController implements ErrorController {
	private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

	@Value(value = "${git.commit.id.abbrev}")
	private String gitCommitId;

	@Value(value = "${git.build.time}")
	private String gitBuildTime;

	@Autowired
	private ClientService clientService;
	
	@Autowired
	private SofdConfiguration configuration;

	@GetMapping(value = { "/" })
	public String index(Model model) {
		if (SecurityUtil.getUser() != null) {
			model.addAttribute("clients", clientService.findAll().stream()
					.filter(c -> c.isShowOnFrontpage())
					.map(c -> new ClientActivityDTO(c))
					.collect(Collectors.toList()));
		}

		return "index";
	}
	
	@GetMapping("/info")
	public String info(Model model) {
		model.addAttribute("gitBuildTime", gitBuildTime.substring(0, 10));
		model.addAttribute("gitCommitId", gitCommitId);
		model.addAttribute("releaseVersion", configuration.getVersion());
		
		return "info";
	}

	@GetMapping(value = { "/ui/version" })
	public String newVersion(Model model) {
		return "version";
	}

	@RequestMapping(value = "/error", produces = "text/html")
	public String errorPage(Model model, HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		// default to ordinary error message in case error is not SAML related
		model.addAllAttributes(body);

		return "error";
	}

	@RequestMapping(value = "/error", produces = "application/json")
	public ResponseEntity<Map<String, Object>> errorJSON(HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		try {
			status = HttpStatus.valueOf((int) body.get("status"));
		}
		catch (Exception ex) {
			;
		}

		return new ResponseEntity<>(body, status);
	}

	private Map<String, Object> getErrorAttributes(WebRequest request) {
		return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
	}
}
