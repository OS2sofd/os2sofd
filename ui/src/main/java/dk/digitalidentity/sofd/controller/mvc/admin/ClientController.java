package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import dk.digitalidentity.sofd.dao.model.ClientIpAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.ClientDTO;
import dk.digitalidentity.sofd.controller.validation.ClientDTOValidator;
import dk.digitalidentity.sofd.dao.model.AccessField;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntity;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntityField;
import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.ClientService;
import dk.digitalidentity.sofd.service.ClientIpAddressService;

@RequireAdminAccess
@Controller
public class ClientController {

	@Autowired
	private ClientService clientService;

	@Autowired
	private ClientDTOValidator clientDTOValidator;

	@InitBinder
	public void initClientBinder(WebDataBinder binder) {
		binder.setValidator(clientDTOValidator);
	}

	@Autowired
	private ClientIpAddressService clientIpAddressService;

	@GetMapping(path = { "/ui/client", "/ui/client/list" })
	public String list(Model model) {
		List<Client> clients = clientService.findAllButInternals();

		model.addAttribute("clients", clients);
		model.addAttribute("accessRoles", AccessRole.values());
		model.addAttribute("newClient", new ClientDTO());

		return "admin/client/list";
	}

	@GetMapping("/ui/client/getClientList")
	public String getClientList(Model model) {
		List<Client> clients = clientService.findAllButInternals();

		model.addAttribute("clients", clients);

		return "admin/client/list_content :: list";
	}

	@GetMapping("/ui/client/new")
	public String newClient(Model model) {
		ClientDTO clientDTO = new ClientDTO();
		clientDTO.setFieldList("PERSON_BASIC;ORGUNIT_BASIC");

		model.addAttribute("client", clientDTO);
		model.addAttribute("accessRoles", AccessRole.values());
		model.addAttribute("ouFields", AccessEntityField.getAllOrgunitFields());
		model.addAttribute("userFields", AccessEntityField.getAllPersonFields());

		return "admin/client/new";
	}

	@PostMapping("/ui/client/new")
	public String newClientPost(Model model, @Valid @ModelAttribute("client") ClientDTO clientDTO, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("client", clientDTO);
			model.addAttribute("accessRoles", AccessRole.values());
			model.addAttribute("ouFields", AccessEntityField.getAllOrgunitFields());
			model.addAttribute("userFields", AccessEntityField.getAllPersonFields());

			return "admin/client/new";
		}

		Client client = new Client();
		client.setName(clientDTO.getName());
		client.setApiKey(clientDTO.getApiKey());
		client.setVersionStatus(VersionStatus.UNKNOWN);
		client.setAccessRole(AccessRole.valueOf(clientDTO.getAccessRole()));
		client.setAccessFieldList(new ArrayList<>());

		List<String> fields = Arrays.stream(clientDTO.getFieldList().split(";")).distinct().collect(Collectors.toList());
		for (String field : fields) {
			if (!field.isEmpty()) {
				AccessField ae = new AccessField();
				ae.setClient(client);

				ae.setEntity(AccessEntity.valueOf(field.split("_")[0]));
				ae.setAccessEntityField(AccessEntityField.valueOf(field));

				client.getAccessFieldList().add(ae);
			}
		}

		clientService.save(client);

		return "redirect:/ui/client/list";
	}

	@GetMapping("/ui/client/view/{clientId}")
	public String list(@PathVariable long clientId, Model model) throws Exception {
		Client client = clientService.getClientById(clientId);
		if (client == null) {
			return "redirect:/ui/client/list";
		}

		ClientDTO clientDTO = new ClientDTO();
		clientDTO.setId(client.getId());
		clientDTO.setName(client.getName());
		clientDTO.setApiKey(client.getApiKey());
		clientDTO.setAccessRole(client.getAccessRole().getMessageId());

		List<String> orgUnitAccessEntityFieldList = new ArrayList<>();
		List<String> userAccessEntityFieldList = new ArrayList<>();

		for (AccessField field : client.getAccessFieldList()) {
			String fieldName = field.getAccessEntityField().getMessageId();

			if (field.getEntity().equals(AccessEntity.PERSON)) {
				userAccessEntityFieldList.add(fieldName);
			} else if (field.getEntity().equals(AccessEntity.ORGUNIT)) {
				orgUnitAccessEntityFieldList.add(fieldName);
			}
		}

		model.addAttribute("client", clientDTO);
		model.addAttribute("ouFieldList", orgUnitAccessEntityFieldList);
		model.addAttribute("userFieldList", userAccessEntityFieldList);
		model.addAttribute("clientIpAddresses", clientIpAddressService.getAllByClient(client));

		return "admin/client/view";
	}

	@GetMapping("/ui/client/edit/{clientId}")
	public String editClient(@PathVariable long clientId, Model model) {
		Client client = clientService.getClientById(clientId);
		if (client == null) {
			return "redirect:/ui/client/list";
		}

		ClientDTO clientDTO = new ClientDTO();
		clientDTO.setId(clientId);
		clientDTO.setName(client.getName());
		clientDTO.setApiKey(client.getApiKey());
		clientDTO.setAccessRole(client.getAccessRole().name());

		String accessFieldList = null;
		for (AccessField accessField : client.getAccessFieldList()) {
			if (accessFieldList != null) {
				accessFieldList = accessFieldList + ";" + accessField.getAccessEntityField().name();
			} else {
				accessFieldList = accessField.getAccessEntityField().name();
			}

		}

		clientDTO.setFieldList(accessFieldList);

		model.addAttribute("client", clientDTO);
		model.addAttribute("accessRoles", AccessRole.values());
		model.addAttribute("ouFields", AccessEntityField.getAllOrgunitFields());
		model.addAttribute("userFields", AccessEntityField.getAllPersonFields());
		model.addAttribute("clientIpAddresses", clientIpAddressService.getAllByClient(client));

		return "admin/client/edit";
	}

	@PostMapping("/ui/client/edit/{clientId}")
	public String editClientPost(Model model, @Valid @ModelAttribute("client") ClientDTO clientDTO, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("client", clientDTO);
			model.addAttribute("accessRoles", AccessRole.values());
			model.addAttribute("ouFields", AccessEntityField.getAllOrgunitFields());
			model.addAttribute("userFields", AccessEntityField.getAllPersonFields());

			return "admin/client/edit";
		}

		Client client = clientService.getClientById(clientDTO.getId());
		if (client == null) {
			return "redirect:/ui/client/list";
		}

		client.setName(clientDTO.getName());
		client.setApiKey(clientDTO.getApiKey());
		client.setAccessRole(AccessRole.valueOf(clientDTO.getAccessRole()));

		List<AccessField> aeList = new ArrayList<>();

		for (String field : clientDTO.getFieldList().split(";")) {
			if (!field.isEmpty()) {
				AccessField ae = new AccessField();
				ae.setClient(client);

				ae.setEntity(AccessEntity.valueOf(field.split("_")[0]));
				ae.setAccessEntityField(AccessEntityField.valueOf(field));

				aeList.add(ae);
			}
		}

		client.getAccessFieldList().clear();
		client.getAccessFieldList().addAll(aeList);
		client = clientService.save(client);

		return "redirect:/ui/client/list";
	}

	@GetMapping("ui/client/clientIpAddresses/edit/{clientId}")
	public String clientIpAddressesEdit(Model model, @PathVariable long clientId) {
		Client client = clientService.getClientById(clientId);
		if (client == null) {
			return "redirect:/ui/client/list";
		}

		model.addAttribute("clientId", clientId);
		model.addAttribute("clientIpAddresses", clientIpAddressService.getAllByClient(client).stream().map(ClientIpAddress::getIp).toList());
		return "admin/client/editClientIpAddresses";
	}

	@GetMapping("/ui/client/delete/{clientId}")
	public String deleteClient(@PathVariable long clientId) {
		Client client = clientService.getClientById(clientId);
		if (client != null) {
			clientService.delete(client);
		}

		return "redirect:/ui/client/list";
	}
}
