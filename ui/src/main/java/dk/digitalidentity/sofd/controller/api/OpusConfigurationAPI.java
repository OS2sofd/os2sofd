package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.dto.OpusFilterRulesDTO;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.SettingService;

@RequireReadAccess
@RestController
public class OpusConfigurationAPI {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private SettingService settingService;

	@GetMapping("/api/opusfilters")
	public ResponseEntity<?> listFilters() {
		OpusFilterRulesDTO opusFilterRulesDTO = new OpusFilterRulesDTO();
		opusFilterRulesDTO.setEnabled(configuration.getIntegrations().getOpus().isEnableFiltering());
		opusFilterRulesDTO.setLosIds(configuration.getIntegrations().getOpus().getLosIds());
		opusFilterRulesDTO.setPositionIds(configuration.getIntegrations().getOpus().getPositionIds());
		opusFilterRulesDTO.setPositionNames(configuration.getIntegrations().getOpus().getPositionNames());
		opusFilterRulesDTO.setOrgUnitInfix(configuration.getIntegrations().getOpus().getOrgUnitInfix());
		opusFilterRulesDTO.setInvalidPositionNames(configuration.getIntegrations().getOpus().getInvalidPositionNames());
		
		return new ResponseEntity<>(opusFilterRulesDTO, HttpStatus.OK);
	}
	
	@GetMapping("/api/opusautoaffiliations")
	public ResponseEntity<?> listAutoAffiliations() {
		Setting setting = settingService.getOpusAutoAffiliations();

		return new ResponseEntity<>(setting, HttpStatus.OK);
	}
}
