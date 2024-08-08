package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.FunctionFacetAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.FacetDTO;
import dk.digitalidentity.sofd.controller.api.dto.FacetValueDTO;
import dk.digitalidentity.sofd.controller.api.dto.FunctionAssignmentCreateDTO;
import dk.digitalidentity.sofd.controller.api.dto.FunctionAssignmentDTO;
import dk.digitalidentity.sofd.controller.api.dto.FunctionAssignmentEditDTO;
import dk.digitalidentity.sofd.controller.api.dto.FunctionDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Facet;
import dk.digitalidentity.sofd.dao.model.FacetListItem;
import dk.digitalidentity.sofd.dao.model.FacetValue;
import dk.digitalidentity.sofd.dao.model.Function;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.FacetService;
import dk.digitalidentity.sofd.service.FunctionAssignmentService;
import dk.digitalidentity.sofd.service.FunctionService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.ValueData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireDaoWriteAccess
@RestController
public class FunctionHierarchyApiController {
	
	@Autowired
	private FunctionService functionService;
	
	@Autowired
	private FunctionAssignmentService functionAssignmentService;
	
	@Autowired
	private AffiliationService affiliationService;
	
	@Autowired
	private FacetService facetService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private SofdConfiguration sofdConfiguration;
	
	@GetMapping("/api/functionhierarchy/functions")
	public ResponseEntity<?> getFunctionsAndFacets() {
		List<FunctionDTO> result =  functionService.getAll().stream().map(f -> 
			FunctionDTO.builder()
					   .id(f.getId())
					   .sortKey(f.getSortKey())
	                   .name(f.getName())
	                   .category(f.getCategory())
	                   .description(f.getDescription())
	                   .facets(f.getFacetAssignments().stream().map(fa -> FacetDTO.builder()
	                		   											.id(fa.getFacet().getId())
	                    		                                        .name(fa.getFacet().getName())
	                    		                                        .description(fa.getFacet().getDescription())
	                    		                                        .type(fa.getFacet().getType())
	                    		                                        .pattern(fa.getFacet().getPattern())
							   											.sortKey(fa.getSortKey() == null ? Long.MAX_VALUE : fa.getSortKey())
	                    		                                        .listItems(fa.getFacet().getListItems().stream().map(l -> l.getText()).collect(Collectors.toList()))
	                    		                                        .build()).collect(Collectors.toList()))
	                   .build()).collect(Collectors.toList());
		
		Comparator<FunctionDTO> compareBySortKey = (FunctionDTO o1, FunctionDTO o2) -> Integer.compare(o1.getSortKey(), o2.getSortKey());
		Collections.sort(result, compareBySortKey);
		
		return new ResponseEntity<List<FunctionDTO>>(result, HttpStatus.OK);
	}
	
	@GetMapping("/api/functionhierarchy/functionassignments")
	public ResponseEntity<?> getFunctionAssignments(@RequestParam(required = false) String ous) {
		List<FunctionAssignmentDTO> result = new ArrayList<>();
		List<FunctionAssignment> allAssignments = functionAssignmentService.getAll();
		List<FunctionAssignment> assignments = new ArrayList<>();
		if (ous == null) {
			List<String> allUuids = orgUnitService.getAll().stream().map(o -> o.getUuid()).collect(Collectors.toList());
			assignments = allAssignments.stream().filter(f -> allUuids.contains(f.getAffiliation().getCalculatedOrgUnit().getUuid())).collect(Collectors.toList());
		} else {
			List<String> ouUuids = Arrays.asList(ous.split(","));
			List<String> uuidsIncludingChildren = orgUnitService.getAllWithChildren(ouUuids).stream().map(o -> o.getUuid()).collect(Collectors.toList());
			assignments = allAssignments.stream().filter(f -> uuidsIncludingChildren.contains(f.getAffiliation().getCalculatedOrgUnit().getUuid())).collect(Collectors.toList());
		}
		
		for (FunctionAssignment assignment : assignments) {
			FunctionAssignmentDTO functionAssignmentDTO = new FunctionAssignmentDTO();
			functionAssignmentDTO.setId(assignment.getId());
			functionAssignmentDTO.setStartDate(assignment.getStartDate());
			functionAssignmentDTO.setStopDate(assignment.getStopDate());
			functionAssignmentDTO.setAffiliationUuid(assignment.getAffiliation().getUuid());
			functionAssignmentDTO.setAffiliationPersonName(PersonService.getName(assignment.getAffiliation().getPerson()));
			functionAssignmentDTO.setAffiliationPersonUserId(assignment.getAffiliation().getPerson().getPrimeUserByUserType(sofdConfiguration.getModules().getFunctionHierarchy().getDisplayUserType()));
			
			functionAssignmentDTO.setFacetValues(new ArrayList<>());
			for (FunctionFacetAssignment facetAssignment : assignment.getFunction().getFacetAssignments()) {
				Facet facet = facetAssignment.getFacet();
				long sortKey = facetAssignment.getSortKey() == null ? Long.MAX_VALUE : facetAssignment.getSortKey();
				FacetValue match = assignment.getFacetValues().stream().filter(f -> f.getFacet().getId() == facet.getId()).findAny().orElse(null);
				if (match == null) {
					FacetValueDTO dto = FacetValueDTO.builder()
							.facetId(facet.getId())
							.facetName(facet.getName())
							.sortKey(sortKey)
							.build();
					functionAssignmentDTO.getFacetValues().add(dto);
				} else {
					FacetValueDTO dto = FacetValueDTO.builder()
							.facetId(match.getFacet().getId())
							.facetName(match.getFacet().getName())
							.facetListItem(match.getFacetListItem() != null ? match.getFacetListItem().getText() : null)
							.text(match.getText())
							.facetValueOrgunitUuids(match.getOrgUnits().stream().map(o -> o.getUuid()).collect(Collectors.toList()))
							.facetValueOrgunitNames(match.getOrgUnits().stream().map(o -> o.getName()).collect(Collectors.joining(", ")))
							.facetValueAffiliationUuid(match.getAffiliation() != null ? match.getAffiliation().getUuid() : null)
							.facetValueAffiliationPersonName(match.getAffiliation() != null ? PersonService.getName(match.getAffiliation().getPerson()) : null)
							.date(match.getDate())
							.sortKey(sortKey)
							.build();
					functionAssignmentDTO.getFacetValues().add(dto);
				}
			}
			
			functionAssignmentDTO.setFunction(FunctionDTO.builder()
					   .id(assignment.getFunction().getId())
					   .sortKey(assignment.getFunction().getSortKey())
	                   .name(assignment.getFunction().getName())
	                   .category(assignment.getFunction().getCategory())
	                   .description(assignment.getFunction().getDescription())
	                   .facets(assignment.getFunction().getFacetAssignments().stream().map(fa -> FacetDTO.builder()
	                		   											.id(fa.getFacet().getId())
	                    		                                        .name(fa.getFacet().getName())
	                    		                                        .description(fa.getFacet().getDescription())
	                    		                                        .type(fa.getFacet().getType())
	                    		                                        .pattern(fa.getFacet().getPattern())
	                    		                                        .listItems(fa.getFacet().getListItems().stream().map(l -> l.getText()).collect(Collectors.toList()))
	                    		                                        .build()).collect(Collectors.toList()))
	                   .build());

			// inlcude the names of other assignments given to the same affiliation in the same category
			functionAssignmentDTO.setOtherAssignments(
					allAssignments.stream().filter(aa ->
									aa.getId() != functionAssignmentDTO.getId()
									&& aa.getFunction().getCategory().equalsIgnoreCase(functionAssignmentDTO.getFunction().getCategory())
									&& aa.getAffiliation().getUuid().equalsIgnoreCase(functionAssignmentDTO.getAffiliationUuid()))
							.map(a -> a.getFunction().getName()).distinct().sorted().collect(Collectors.joining(", ")));

			result.add(functionAssignmentDTO);
		}
		
		Comparator<FunctionAssignmentDTO> compareBySortKey = (FunctionAssignmentDTO o1, FunctionAssignmentDTO o2) -> Integer.compare(o1.getFunction().getSortKey(), o2.getFunction().getSortKey());
		Collections.sort(result, compareBySortKey);
		
		return new ResponseEntity<List<FunctionAssignmentDTO>>(result, HttpStatus.OK);
	}
	
	@PostMapping("/api/functionhierarchy/functionassignments/create")
	public ResponseEntity<?> createFunctionAssignment(@RequestBody FunctionAssignmentCreateDTO dto) {
		if (!StringUtils.hasLength(dto.getAffiliationUuid()) || dto.getFunctionId() == 0) {
			return new ResponseEntity<>("Et eller flere krævede felter mangler.", HttpStatus.BAD_REQUEST);
		}
		
		if (dto.getStartDate() != null && dto.getStopDate() != null && dto.getStartDate().isAfter(dto.getStopDate())) {
			return new ResponseEntity<>("Startdatoen skal være før stopdatoen.", HttpStatus.BAD_REQUEST);
		}
		
		Affiliation affiliation = affiliationService.findByUuid(dto.getAffiliationUuid());
		if (affiliation == null) {
			return new ResponseEntity<>("Den affiliation, der forsøges at lave en funktionstildeling til, eksisterer ikke.", HttpStatus.BAD_REQUEST);
		}
		
		Function function = functionService.getById(dto.getFunctionId());
		if (function == null) {
			return new ResponseEntity<>("Den funktion, der forsøges at lave en funktionstildeling til, eksisterer ikke.", HttpStatus.BAD_REQUEST);
		}
		
		FunctionAssignment functionAssignment = new FunctionAssignment();
		functionAssignment.setAffiliation(affiliation);
		functionAssignment.setFunction(function);
		functionAssignment.setStartDate(dto.getStartDate());
		functionAssignment.setStopDate(dto.getStopDate());
		functionAssignment.setFacetValues(new ArrayList<>());
		
		for (FacetValueDTO facetValueDTO : dto.getFacetValues()) {
			FacetValue facetValue = new FacetValue();
			Facet facet = facetService.getById(facetValueDTO.getFacetId());
			if (facet == null) {
				log.warn("Facet with id " + facetValueDTO.getFacetId() + " was not found when creating facetValue for new functionAssignment. Will not create this facetValue.");
				continue;
			}
			
			facetValue.setFacet(facet);
			
			if (facet.getType().equals(FacetType.FREETEXT)) {
				if (facet.getPattern() != null && !facet.getPattern().isEmpty()) {
					if (!facetValueDTO.getText().matches(facet.getPattern())) {
						log.warn("FacetValue " + facetValueDTO.getText() + " does not match pattern for Facet with id " + facet.getId() + ".");
						return new ResponseEntity<>("FacetValue " + facetValueDTO.getText() + " mathcer ikke pattern for Facet med id " + facet.getId() + ".", HttpStatus.BAD_REQUEST);
					}
				}
				facetValue.setText(facetValueDTO.getText());
			} else if (facet.getType().equals(FacetType.LIST)) {
				FacetListItem item = facet.getListItems().stream().filter(l -> l.getText().equals(facetValueDTO.getFacetListItem())).findAny().orElse(null);
				if (item == null) {
					log.warn("Unknown list item " + facetValueDTO.getFacetListItem() + " for facet with id " + facet.getId() + " when creating facetValue for new functionAssignment. Will set listItem to null.");
				}
				facetValue.setFacetListItem(item);
			} else if (facet.getType().equals(FacetType.ORG)) {
				facetValue.setOrgUnits(new ArrayList<>());
				for (String ouUuid : facetValueDTO.getFacetValueOrgunitUuids()) {
					OrgUnit ou = orgUnitService.getByUuid(ouUuid);
					if (ou == null) {
						log.warn("Unknown ou " + ouUuid + " when creating facetValue for new functionAssignment. Will not add this ou to facetValue.");
						continue;
					}
					facetValue.getOrgUnits().add(ou);
				}
			} else if (facet.getType().equals(FacetType.EMPLOYEE)) {
				Affiliation facetAffiliation = affiliationService.findByUuid(facetValueDTO.getFacetValueAffiliationUuid());
				if (facetAffiliation == null) {
					log.warn("Unknown affiliation " + facetValueDTO.getFacetValueAffiliationUuid() + " for facet with id " + facet.getId() + " when creating facetValue for new functionAssignment. Will set affiliation facet value to null.");
				}
				facetValue.setAffiliation(facetAffiliation);
			} else if (facet.getType().equals(FacetType.FOLLOW_UP_DATE)) {
				facetValue.setDate(facetValueDTO.getDate());
			} else {
				log.warn("Unknown facet type " + facet.getType() + " on facet with id " + facet.getId() + " when creating facetValue for new functionAssignment. Will not create this facetValue.");
				continue;
			}
			
			facetValue.setFunctionAssignment(functionAssignment);
			functionAssignment.getFacetValues().add(facetValue);
		}
		
		functionAssignmentService.save(functionAssignment);
		
		log.info("Created new functionAssignment");
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/api/functionhierarchy/functionassignments/{id}/edit")
	public ResponseEntity<?> editFunctionAssignment(@RequestBody FunctionAssignmentEditDTO dto, @PathVariable long id) {
		FunctionAssignment functionAssignment = functionAssignmentService.getById(id);
		if (functionAssignment == null) {
			return new ResponseEntity<>("Funktionstildeling med id " + id + " findes ikke.", HttpStatus.NOT_FOUND);
		}

		if (dto.getStartDate() != null && dto.getStopDate() != null && dto.getStartDate().isAfter(dto.getStopDate())) {
			return new ResponseEntity<>("Startdatoen skal være før stopdatoen.", HttpStatus.BAD_REQUEST);
		}

		boolean changes = false;
		
		if (!Objects.equals(functionAssignment.getStartDate(),dto.getStartDate())) {
			changes = true;
			functionAssignment.setStartDate(dto.getStartDate());
		}
		
		if (!Objects.equals(functionAssignment.getStopDate(),dto.getStopDate())) {
			changes = true;
			functionAssignment.setStopDate(dto.getStopDate());
		}
		
		List<Long> existingFacetIds = functionAssignment.getFacetValues().stream().map(f -> f.getFacet().getId()).collect(Collectors.toList());
		for (FacetValueDTO facetValueDTO : dto.getFacetValues()) {
			if (!existingFacetIds.contains(facetValueDTO.getFacetId())) {
				// create
				FacetValue facetValue = new FacetValue();
				Facet facet = facetService.getById(facetValueDTO.getFacetId());
				if (facet == null) {
					log.warn("Facet with id " + facetValueDTO.getFacetId() + " was not found when adding facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not create this facetValue.");
					continue;
				}
				
				facetValue.setFacet(facet);
				
				if (facet.getType().equals(FacetType.FREETEXT)) {
					facetValue.setText(facetValueDTO.getText());
				} else if (facet.getType().equals(FacetType.LIST)) {
					FacetListItem item = facet.getListItems().stream().filter(l -> l.getText().equals(facetValueDTO.getFacetListItem())).findAny().orElse(null);
					if (item == null) {
						log.warn("Unknown list item " + facetValueDTO.getFacetListItem() + " for facet with id " + facet.getId() + " when adding facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not add this facetValue.");
						continue;
					}
					facetValue.setFacetListItem(item);
				} else if (facet.getType().equals(FacetType.ORG)) {
					facetValue.setOrgUnits(new ArrayList<>());
					for (String ouUuid : facetValueDTO.getFacetValueOrgunitUuids()) {
						OrgUnit ou = orgUnitService.getByUuid(ouUuid);
						if (ou == null) {
							log.warn("Unknown ou " + ouUuid + " when adding facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not add this ou to facetValue.");
							continue;
						}
						facetValue.getOrgUnits().add(ou);
					}
				} else if (facet.getType().equals(FacetType.EMPLOYEE)) {
					Affiliation facetAffiliation = affiliationService.findByUuid(facetValueDTO.getFacetValueAffiliationUuid());
					if (facetAffiliation == null) {
						log.warn("Unknown affiliation " + facetValueDTO.getFacetValueAffiliationUuid() + " for facet with id " + facet.getId() + " when adding facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will set affiliation facet value to null.");
						continue;
					}
					
					facetValue.setAffiliation(facetAffiliation);
				} else if (facet.getType().equals(FacetType.FOLLOW_UP_DATE)) {
					facetValue.setDate(facetValueDTO.getDate());
				}
				else {
					log.warn("Unknown facet type " + facet.getType() + " on facet with id " + facet.getId() + " when adding facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not create this facetValue.");
					continue;
				}
				
				facetValue.setFunctionAssignment(functionAssignment);
				functionAssignment.getFacetValues().add(facetValue);
				
				changes = true;
			} else {
				
				// update
				FacetValue facetValueToUpdate = functionAssignment.getFacetValues().stream().filter(f -> f.getFacet().getId() == facetValueDTO.getFacetId()).findAny().orElse(null);
				
				// should never be null, but making sure
				if (facetValueToUpdate == null) {
					continue;
				}
				
				Facet facet = facetValueToUpdate.getFacet();
				
				if (facet.getType().equals(FacetType.FREETEXT)) {
					if (!facetValueToUpdate.getText().equals(facetValueDTO.getText())) {
						facetValueToUpdate.setText(facetValueDTO.getText());
						changes = true;
					}
				} else if (facet.getType().equals(FacetType.LIST)) {
					if (facetValueToUpdate.getFacetListItem() == null || !facetValueToUpdate.getFacetListItem().getText().equals(facetValueDTO.getFacetListItem())) {
						FacetListItem item = facet.getListItems().stream().filter(l -> l.getText().equals(facetValueDTO.getFacetListItem())).findAny().orElse(null);
						if (item == null) {
							log.warn("Unknown list item " + facetValueDTO.getFacetListItem() + " for facet with id " + facet.getId() + " when editing facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not edit this facetValue.");
							continue;
						}
						facetValueToUpdate.setFacetListItem(item);
						changes = true;
					}
				} else if (facet.getType().equals(FacetType.ORG)) {
					List<String> existingOUs = facetValueToUpdate.getOrgUnits().stream().map(o -> o.getUuid()).collect(Collectors.toList());
					for (String ouUuid : facetValueDTO.getFacetValueOrgunitUuids()) {
						if (!existingOUs.contains(ouUuid)) {
							OrgUnit ou = orgUnitService.getByUuid(ouUuid);
							if (ou == null) {
								log.warn("Unknown ou " + ouUuid + " when editing facetValue for new functionAssignment with id " + functionAssignment.getId() + ". Will not add this ou to facetValue.");
								continue;
							}
							facetValueToUpdate.getOrgUnits().add(ou);
							changes = true;
						}
					}
					
					Iterator<OrgUnit> iterator = facetValueToUpdate.getOrgUnits().iterator();
					OrgUnit temp = null;
				    while (iterator.hasNext()) {
				    	temp = (OrgUnit) iterator.next();
				    	if (!facetValueDTO.getFacetValueOrgunitUuids().contains(temp.getUuid())) {
				    		changes = true;
				    		iterator.remove();
				    	}
				    }
				} else if (facet.getType().equals(FacetType.EMPLOYEE)) {
					if (!StringUtils.hasLength(facetValueDTO.getFacetValueAffiliationUuid()) && facetValueToUpdate.getAffiliation() != null) {
						facetValueToUpdate.setAffiliation(null);
						changes = true;
					} else if ((facetValueToUpdate.getAffiliation() == null && StringUtils.hasLength(facetValueDTO.getFacetValueAffiliationUuid())) || (StringUtils.hasLength(facetValueDTO.getFacetValueAffiliationUuid()) && !facetValueDTO.getFacetValueAffiliationUuid().equals(facetValueToUpdate.getAffiliation().getUuid()))) {
						Affiliation facetAffiliation = affiliationService.findByUuid(facetValueDTO.getFacetValueAffiliationUuid());
						if (facetAffiliation == null) {
							log.warn("Unknown affiliation " + facetValueDTO.getFacetValueAffiliationUuid() + " for facet with id " + facet.getId() + " when editing facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not edit this facetValue.");
							continue;
						}
						facetValueToUpdate.setAffiliation(facetAffiliation);
						changes = true;
					}
				} else if (facet.getType().equals(FacetType.FOLLOW_UP_DATE)) {
					if (!Objects.equals(facetValueToUpdate.getDate(),facetValueDTO.getDate())) {
						facetValueToUpdate.setDate(facetValueDTO.getDate());
						changes = true;
					}
				} else {
					log.warn("Unknown facet type " + facet.getType() + " on facet with id " + facet.getId() + " when editing facetValue for functionAssignment with id " + functionAssignment.getId() + ". Will not edit this facetValue.");
					continue;
				}
			}
			
		}
		
		// handle delete facetValue
		List<Long> facetIds = dto.getFacetValues().stream().map(f -> f.getFacetId()).collect(Collectors.toList());
		Iterator<FacetValue> iterator = functionAssignment.getFacetValues().iterator();
		FacetValue temp = null;
	    while (iterator.hasNext()) {
	    	temp = (FacetValue) iterator.next();
	    	if (!facetIds.contains(temp.getFacet().getId())) {
	    		changes = true;
	    		iterator.remove();
	    	}
	    }
		
		if (changes) {
			functionAssignmentService.save(functionAssignment);
			log.info("Updated funtionAssignment with id " + id);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/api/functionhierarchy/functionassignments/{id}/delete")
	public ResponseEntity<?> editFunctionAssignment(@PathVariable long id) {
		FunctionAssignment functionAssignment = functionAssignmentService.getById(id);
		if (functionAssignment == null) {
			return new ResponseEntity<>("Funktionstildeling med id " + id + " findes ikke.", HttpStatus.NOT_FOUND);
		}
		
		functionAssignmentService.delete(functionAssignment);
		
		log.info("Deleted funtionAssignment with id " + id);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@GetMapping(value = "/api/functionhierarchy/search/person")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String term, @RequestParam String ous) {
		List<String> ouUuids = Arrays.asList(ous.split(","));
		List<OrgUnit> orgUnitsIncludingChildren = orgUnitService.getAllWithChildren(ouUuids);


		// TODO: det her virker, men der kan måske laves noget fancy sql

		List<Affiliation> affiliations = new ArrayList<>();
		List<List<Affiliation>> affiliationsLists = orgUnitsIncludingChildren.stream().map(o -> o.getAffiliations()).collect(Collectors.toList());
		for (List<Affiliation> affiliationsList : affiliationsLists) {

			affiliations.addAll(AffiliationService.onlyActiveAffiliations(affiliationsList));
		}
		
		affiliations = affiliations.stream().filter(a -> PersonService.getName(a.getPerson()).toLowerCase().contains(term.toLowerCase())).collect(Collectors.toList());

		List<ValueData> suggestions = new ArrayList<>();
		for (Affiliation affiliation : affiliations) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(affiliation.getPerson()));

			builder.append(" (" + AffiliationService.getPositionName(affiliation) + " i " + affiliation.getCalculatedOrgUnit().getName() + ")");

			ValueData vd = new ValueData();
			vd.setValue(builder.toString());
			vd.setData(affiliation.getUuid());

			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping(value = "/api/functionhierarchy/search/all")
	public ResponseEntity<?> searchPerson(@RequestParam String ous) {
		List<String> ouUuids = Arrays.asList(ous.split(","));
		List<OrgUnit> orgUnitsIncludingChildren = orgUnitService.getAllWithChildren(ouUuids);
		
		List<Affiliation> affiliations = new ArrayList<>();
		List<List<Affiliation>> affiliationsLists = orgUnitsIncludingChildren.stream().map(o -> o.getAffiliations()).collect(Collectors.toList());
		for (List<Affiliation> affiliationsList : affiliationsLists) {

			affiliations.addAll(AffiliationService.onlyActiveAffiliations(affiliationsList));
		}
		
		List<ValueData> suggestions = new ArrayList<>();
		for (Affiliation affiliation : affiliations) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(affiliation.getPerson()));

			builder.append(" (" + AffiliationService.getPositionName(affiliation) + " i " + affiliation.getCalculatedOrgUnit().getName() + ")");

			ValueData vd = new ValueData();
			vd.setValue(builder.toString());
			vd.setData(affiliation.getUuid());

			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping("/api/functionhierarchy/ous")
	public ResponseEntity<?> getOus() {
		return new ResponseEntity<List<OUTreeForm>>(orgUnitService.getAllTree(), HttpStatus.OK);
	}
}
