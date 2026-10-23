package dk.digitalidentity.sofd.controller.rest.admin;

import dk.digitalidentity.sofd.controller.rest.admin.model.ProfessionDto;
import dk.digitalidentity.sofd.dao.model.Profession;
import dk.digitalidentity.sofd.dao.model.mapping.ProfessionMapping;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.ProfessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RequireAdminAccess
@RestController
public class ProfessionRestController {

    @Autowired
    private ProfessionService professionService;

    @Autowired
    private OrganisationService organisationService;

    @GetMapping("/rest/admin/professions")
    public ResponseEntity<?> getProfessions(@RequestParam("organisationId") long organisationId) {
        var professions = professionService.getByOrganisationId(organisationId);
        return ResponseEntity.ok(professions.stream().map(ProfessionDto::new));
    }

    @GetMapping("/rest/admin/professions/uniquePositionNames")
    public ResponseEntity<?> getUniquePositionNames(@RequestParam("organisationId") long organisationId) {
        var positionNames = professionService.getUniquePositionNames(organisationId);
        return ResponseEntity.ok(positionNames);
    }

    @GetMapping("/rest/admin/professions/translations")
    public ResponseEntity<?> getTranslations(@RequestParam("organisationId") long organisationId) {
        var translations = professionService.getTranslations(organisationId);
        return ResponseEntity.ok(translations);
    }

    @PostMapping("/rest/admin/professions/createInitial")
    public ResponseEntity<?> getUniquePositionNames(@RequestParam("organisationId") long organisationId,@RequestBody List<String> positionNames) {
        var organisation = organisationService.getById(organisationId);
        if( organisation == null ) {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
        var professions = professionService.getByOrganisationId(organisationId);
        if( !professions.isEmpty() ) {
            // can only generate initial professions if none exist
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }

        for( var positionName : positionNames ) {
            var name = positionName == null ? null : positionName.strip();
            if( name == null || name.isEmpty() ) {
                continue;
            }
            var profession = new Profession();
            profession.setOrganisationId(organisationId);
            profession.setName(name);
            professionService.save(profession);
        }
        return ResponseEntity.ok(positionNames);
    }


    @PostMapping("/rest/admin/professions")
    public ResponseEntity<String> saveProfession(@RequestBody ProfessionDto professionDto) {
        var organisation = organisationService.getById(professionDto.getOrganisationId());
        if( organisation == null ) {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }

        var name = professionDto.getName() == null ? null : professionDto.getName().strip();
        if( name == null || name.isEmpty() ) {
            return new ResponseEntity<String>("Stillingen skal have et navn", HttpStatus.BAD_REQUEST);
        }

        var profession = professionService.findById(professionDto.getId());
        if( profession == null ) {
            profession = new Profession();
            profession.setOrganisationId(professionDto.getOrganisationId());
        }
        // verify that we don't create dubplicate professions (covers both the new and the edit scenario)
        if( !name.equalsIgnoreCase(profession.getName())) {
            if( professionService.professionNameExists(professionDto.getOrganisationId(), name) ) {
                return new ResponseEntity<String>("Stillingen '" + name + "' findes allerede!", HttpStatus.BAD_REQUEST);
            }
        }
        profession.setName(name);

        // remove deleted mappings
        profession.getProfessionMappings().removeIf( existing -> professionDto.getMappings().stream().noneMatch(dto -> Objects.equals(existing.getId(), dto.getId())));

        // add any new mappings
        for( var dtoMapping : professionDto.getMappings().stream().filter(m -> m.getId() == 0).toList() ) {
            var newMapping = new ProfessionMapping();
            newMapping.setProfession(profession);
            newMapping.setMatchValue(dtoMapping.getMatchValue() == null ? null : dtoMapping.getMatchValue().strip());
            newMapping.setMatchType(dtoMapping.getMatchType());
            profession.getProfessionMappings().add(newMapping);
        }
        professionService.save(profession);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @DeleteMapping("/rest/admin/professions/{professionId}")
    public ResponseEntity<String> deleteProfession(@PathVariable("professionId") long professionId) {
        professionService.deleteById(professionId);
        return new ResponseEntity<String>(HttpStatus.OK);
    }



}
