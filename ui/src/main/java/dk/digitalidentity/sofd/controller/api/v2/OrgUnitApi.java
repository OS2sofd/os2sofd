package dk.digitalidentity.sofd.controller.api.v2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.v2.model.OrgUnitApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.OrgUnitResult;
import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.MasteredEntity;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.mapping.MappableEntity;
import dk.digitalidentity.sofd.dao.model.mapping.MappedEntity;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.ManagerService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireReadAccess
public class OrgUnitApi {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private ManagerService managerService;

	@GetMapping("/api/v2/orgUnits")
	public OrgUnitResult getOrgUnits(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "offset", required = false, defaultValue = "") String offset, @RequestParam(name = "size", defaultValue = "100") int size) {
		List<OrgUnit> orgUnits = null;
		
		if (!StringUtils.hasText(offset) && page > 0) {
			log.warn("/api/v2/orgUnits called with page - this is bad performance and the caller should switch to offset");
			orgUnits = orgUnitService.getAll(PageRequest.of(page, size, Sort.by("uuid"))).getContent();
		}
		else {
			orgUnits = orgUnitService.getByOffsetAndLimit(offset, size);
		}

		OrgUnitResult result = new OrgUnitResult();
		result.setOrgUnits(new HashSet<OrgUnitApiRecord>());
		result.setPage(page);
		if (orgUnits.size() > 0) {
			result.setNextOffset(orgUnits.get(orgUnits.size() - 1).getUuid());
		}
		
		for (OrgUnit orgUnit : orgUnits) {
			result.getOrgUnits().add(new OrgUnitApiRecord(orgUnit));
		}
		
		return result;
	}
	
	@GetMapping("/api/v2/orgUnits/byMasterId/{masterId}")
	public ResponseEntity<?> getOrgUnitByMasterId(@PathVariable("masterId") String masterId) {
		OrgUnit orgUnit = orgUnitService.getByMasterId(masterId);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new OrgUnitApiRecord(orgUnit), HttpStatus.OK);
	}

	@GetMapping("/api/v2/orgUnits/{uuid}")
	public ResponseEntity<?> getOrgUnit(@PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new OrgUnitApiRecord(orgUnit), HttpStatus.OK);
	}

	@RequireApiWriteAccess
	@PostMapping("/api/v2/orgUnits")
	public ResponseEntity<?> createOrgUnit(@Valid @RequestBody OrgUnitApiRecord record, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		if (orgUnitService.getByUuid(record.getUuid()) != null) {
			return new ResponseEntity<>("Already exists", HttpStatus.CONFLICT);
		}

		OrgUnit orgUnit = orgUnitService.save(record.toOrgUnit(null));
		
		return new ResponseEntity<>(new OrgUnitApiRecord(orgUnit), HttpStatus.CREATED);
	}

	@RequireApiWriteAccess
	@PatchMapping("/api/v2/orgUnits/{uuid}")
	public ResponseEntity<?> patchOrgUnit(@PathVariable("uuid") String uuid, @RequestBody OrgUnitApiRecord record, BindingResult bindingResult) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (orgUnit.isBlockUpdate()) {
			log.warn("Attempt to update blocked OrgUnit: {} ({})", orgUnit.getName(), orgUnit.getUuid());
			return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
		}

		boolean changes = patch(orgUnit, record);
		if (!changes) {
			return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
		}

		orgUnit = orgUnitService.save(orgUnit);
		
		return new ResponseEntity<>(new OrgUnitApiRecord(orgUnit), HttpStatus.OK);
	}

	// TODO: why is this GET and not PUT/POST?
	@RequireApiWriteAccess
	@GetMapping("/api/v2/orgUnits/{uuid}/clearManager")
	public ResponseEntity<?> clearManager(@PathVariable("uuid") String uuid) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (orgUnit.getManager() != null) {
			managerService.sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, orgUnit.getManager().getManager());
		}

		orgUnit.setSelectedManagerUuid(null);
		orgUnitService.save(orgUnit);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private boolean patch(OrgUnit orgUnit, OrgUnitApiRecord orgUnitRecord) throws Exception {
		OrgUnit record = orgUnitRecord.toOrgUnit(orgUnit);
		boolean changes = false;
		
		// in patch() fields are only updated if the supplied record is non-null, meaning PATCH cannot
		// null a field - a PUT operation must be implemented for null'ing to be possible.

		if (record.getMaster() != null && !Objects.equals(record.getMaster(), orgUnit.getMaster())) {
			orgUnit.setMaster(record.getMaster());
			changes = true;
		}

		if (record.getMasterId() != null && !Objects.equals(record.getMasterId(), orgUnit.getMasterId())) {
			orgUnit.setMasterId(record.getMasterId());
			changes = true;
		}
		
		if (record.isDeleted() != orgUnit.isDeleted()) {
			orgUnit.setDeleted(record.isDeleted());
			changes = true;
		}

		if (record.getShortname() != null && !Objects.equals(record.getShortname(), orgUnit.getShortname())) {
			orgUnit.setShortname(record.getShortname());
			changes = true;
		}
		
		if (record.getDisplayName() != null && !Objects.equals(record.getDisplayName(), orgUnit.getDisplayName())) {
			orgUnit.setDisplayName(record.getDisplayName());
			changes = true;
		}

		if (record.getSourceName() != null && !Objects.equals(record.getSourceName(), orgUnit.getSourceName())) {
			orgUnit.setSourceName(record.getSourceName());
			changes = true;
		}

		if (record.getCvr() != null && !Objects.equals(record.getCvr(), orgUnit.getCvr())) {
			orgUnit.setCvr(record.getCvr());
			changes = true;
		}
		
		if (record.getCvrName() != null && !Objects.equals(record.getCvrName(), orgUnit.getCvrName())) {
			orgUnit.setCvrName(record.getCvrName());
			changes = true;
		}
		
		if (record.getPnr() != null && !Objects.equals(record.getPnr(), orgUnit.getPnr())) {
			orgUnit.setPnr(record.getPnr());
			changes = true;
		}
		
		if (record.getSenr() != null && !Objects.equals(record.getSenr(), orgUnit.getSenr())) {
			orgUnit.setSenr(record.getSenr());
			changes = true;
		}
		
		if (record.getCostBearer() != null && !Objects.equals(record.getCostBearer(), orgUnit.getCostBearer())) {
			orgUnit.setCostBearer(record.getCostBearer());
			changes = true;
		}
		
		if (record.getOrgType() != null && !Objects.equals(record.getOrgType(), orgUnit.getOrgType())) {
			orgUnit.setOrgType(record.getOrgType());
			changes = true;
		}
		
		if (record.getOrgTypeId() != null && !Objects.equals(record.getOrgTypeId(), orgUnit.getOrgTypeId())) {
			orgUnit.setOrgTypeId(record.getOrgTypeId());
			changes = true;
		}

		if (record.getLocalExtensions() != null && !Objects.equals(record.getLocalExtensions(), orgUnit.getLocalExtensions())) {
			orgUnit.setLocalExtensions(record.getLocalExtensions());
			changes = true;
		}

		if (record.getEmail() != null && !Objects.equals(record.getEmail(), orgUnit.getEmail())) {
			orgUnit.setEmail(record.getEmail());
			changes = true;
		}

		// TODO: it is not possible to set a new root using PATCH... we need to do this manually when it
		//       happens, or implement some PUT procedure for this
		if (record.getParent() != null) {
			if (orgUnit.getParent() == null) {
				orgUnit.setParent(record.getParent());
				changes = true;
			}
			else {
				if (!orgUnit.getParent().getUuid().equals(record.getParent().getUuid())) {
					orgUnit.setParent(record.getParent());
					changes = true;
				}
			}
		}
		
		// due to the way patching works, it is not possible "null" a collection using the PATCH operation,
		// an empty collection must be supplied to "empty" it.

		if (record.getPostAddresses() != null) {
			if (this.<OrgUnitPostMapping>patchCollection(orgUnit, record, OrgUnit.class.getMethod("getPostAddresses"), OrgUnit.class.getMethod("setPostAddresses", List.class))) {
				changes = true;
			}
		}
		
		if (record.getPhones() != null) {
			if (this.<OrgUnitPhoneMapping>patchCollection(orgUnit, record, OrgUnit.class.getMethod("getPhones"), OrgUnit.class.getMethod("setPhones", List.class))) {
				changes = true;
			}
		}

		if (record.getEanList() != null) {
			if (this.<Ean>patchCollection(orgUnit, record, OrgUnit.class.getMethod("getEanList"), OrgUnit.class.getMethod("setEanList", List.class))) {
				changes = true;
			}
		}

		// if there are changes, flip any delete flag (unless the change was an actual delete of course ;))
		if (changes && !record.isDeleted()) {
			orgUnit.setDeleted(false);
		}

		return changes;
	}

	@SuppressWarnings("unchecked")
	private <T extends MappableEntity> boolean patchCollection(OrgUnit orgUnit, OrgUnit record, Method getCollectionMethod, Method setCollectionMethod) throws Exception {
		boolean changes = false;
		
		// record has no entries, orgUnit does
		Collection<T> recordCollection = (Collection<T>) getCollectionMethod.invoke(record);
		Collection<T> orgUnitCollection = (Collection<T>) getCollectionMethod.invoke(orgUnit);
		
		if (recordCollection == null || recordCollection.size() == 0) {
			if (orgUnitCollection != null && orgUnitCollection.size() > 0) {
				for (Iterator<T> iterator = orgUnitCollection.iterator(); iterator.hasNext();) {
					iterator.next();
					iterator.remove();
				}
				
				changes = true;
			}
		}
		else { // record has entries in all of the below cases
			
			// existing OrgUnit does not have any entries
			if (orgUnitCollection == null || orgUnitCollection.size() == 0) {
				if (orgUnitCollection == null) {
					setCollectionMethod.invoke(orgUnit, new ArrayList<T>());
				}

				for (T recordEntry : recordCollection) {
					orgUnitCollection.add(recordEntry);
				}
				
				changes = true;
			}
			else {
				// both have entries, the big comparison case
				
				// to add or update
				for (T recordEntry : recordCollection) {
					boolean found = false;
					
					for (T orgUnitEntry : orgUnitCollection) {
						MasteredEntity recordMasteredEntity, orgUnitMasteredEntity;

						if (orgUnitEntry instanceof MappedEntity) {
							recordMasteredEntity = ((MappedEntity)recordEntry).getEntity();
							orgUnitMasteredEntity = ((MappedEntity)orgUnitEntry).getEntity();
						}
						else {
							recordMasteredEntity = (MasteredEntity) recordEntry;
							orgUnitMasteredEntity = (MasteredEntity) orgUnitEntry;
						}

						if (Objects.equals(orgUnitMasteredEntity.getMaster(), recordMasteredEntity.getMaster()) &&
							Objects.equals(orgUnitMasteredEntity.getMasterId(), recordMasteredEntity.getMasterId())) {

							if (orgUnitMasteredEntity instanceof Phone) {
								if (patchPhoneEntityFields((Phone) orgUnitMasteredEntity, (Phone) recordMasteredEntity)) {
									changes = true;
								}
							}
							else if (orgUnitMasteredEntity instanceof Post) {
								if (patchPostEntityFields((Post) orgUnitMasteredEntity, (Post) recordMasteredEntity)) {
									changes = true;
								}
							}
							else if (orgUnitMasteredEntity instanceof Ean) {
								// we never update EAN-numbers because the only field is the masterId(number)
							}

							found = true;
							break;
						}
					}
					
					// add if it does not exist
					if (!found) {
						orgUnitCollection.add(recordEntry);
						changes = true;
					}
				}
				
				// to remove
				for (Iterator<T> iterator = orgUnitCollection.iterator(); iterator.hasNext();) {
					T orgUnitEntry = iterator.next();
					boolean found = false;

					for (T recordEntry : recordCollection) {
						MasteredEntity recordMasteredEntity, orgUnitMasteredEntity;

						if (recordEntry instanceof MappedEntity) {
							recordMasteredEntity = ((MappedEntity)recordEntry).getEntity();
							orgUnitMasteredEntity = ((MappedEntity)orgUnitEntry).getEntity();
						}
						else {
							recordMasteredEntity = (MasteredEntity) recordEntry;
							orgUnitMasteredEntity = (MasteredEntity) orgUnitEntry;
						}

						if (Objects.equals(orgUnitMasteredEntity.getMaster(), recordMasteredEntity.getMaster()) &&
							Objects.equals(orgUnitMasteredEntity.getMasterId(), recordMasteredEntity.getMasterId())) {
							
							found = true;
							break;
						}
					}
					
					// add if it does not exist
					if (!found) {
						iterator.remove();
						changes = true;
					}
				}
			}
		}

		return changes;
	}

	private boolean patchPostEntityFields(Post orgUnitEntry, Post recordPost) {
		boolean changes = false;

		// note that patching cannot be used for null'ing fields, only setting or updating them

		if (recordPost.getCity() != null && !Objects.equals(orgUnitEntry.getCity(), recordPost.getCity())) {
			orgUnitEntry.setCity(recordPost.getCity());
			changes = true;
		}
		
		if (recordPost.getCountry() != null && !Objects.equals(orgUnitEntry.getCountry(), recordPost.getCountry())) {
			orgUnitEntry.setCountry(recordPost.getCountry());
			changes = true;
		}
		
		if (recordPost.getLocalname() != null && !Objects.equals(orgUnitEntry.getLocalname(), recordPost.getLocalname())) {
			orgUnitEntry.setLocalname(recordPost.getLocalname());
			changes = true;
		}
		
		if (recordPost.getPostalCode() != null && !Objects.equals(orgUnitEntry.getPostalCode(), recordPost.getPostalCode())) {
			orgUnitEntry.setPostalCode(recordPost.getPostalCode());
			changes = true;
		}
		
		if (recordPost.getStreet() != null && !Objects.equals(orgUnitEntry.getStreet(), recordPost.getStreet())) {
			orgUnitEntry.setStreet(recordPost.getStreet());
			changes = true;
		}
		
		if (recordPost.isAddressProtected() != orgUnitEntry.isAddressProtected()) {
			orgUnitEntry.setAddressProtected(recordPost.isAddressProtected());
			changes = true;
		}
		
		return changes;
	}

	private boolean patchPhoneEntityFields(Phone orgUnitEntry, Phone recordEntry) {
		boolean changes = false;

		// note that patching cannot be used for null'ing fields, only setting or updating them

		if (recordEntry.getFunctionType() != null && !Objects.equals(orgUnitEntry.getFunctionType(), recordEntry.getFunctionType())) {
			orgUnitEntry.setFunctionType(recordEntry.getFunctionType());
			changes = true;
		}

		if (recordEntry.getPhoneNumber() != null && !Objects.equals(orgUnitEntry.getPhoneNumber(), recordEntry.getPhoneNumber())) {
			orgUnitEntry.setPhoneNumber(recordEntry.getPhoneNumber());
			changes = true;
		}

		if (recordEntry.getPhoneType() != null && !Objects.equals(orgUnitEntry.getPhoneType(), recordEntry.getPhoneType())) {
			orgUnitEntry.setPhoneType(recordEntry.getPhoneType());
			changes = true;
		}

		if (recordEntry.getVisibility() != null && !Objects.equals(orgUnitEntry.getVisibility(), recordEntry.getVisibility())) {
			orgUnitEntry.setVisibility(recordEntry.getVisibility());
			changes = true;
		}
		
		return changes;
	}

}
