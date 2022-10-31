package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.FacetDTO;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.FunctionDTO;
import dk.digitalidentity.sofd.dao.model.Facet;
import dk.digitalidentity.sofd.dao.model.FacetListItem;
import dk.digitalidentity.sofd.dao.model.Function;
import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import dk.digitalidentity.sofd.service.FacetService;
import dk.digitalidentity.sofd.service.FunctionService;
import dk.digitalidentity.sofd.service.SettingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class FunctionHierarchyController {
	
	@Autowired
    private FunctionService functionService;
	
	@Autowired
    private FacetService facetService;
	
	@Autowired
	private SofdConfiguration config;
	
	@Autowired
	private SettingService settingService;
	
    @GetMapping("/ui/admin/functionhierarchy/functions")
    public String listFunctions(Model model) {
    	if (!config.getModules().getFunctionHierarchy().isEnabled()) {
    		return "error";
    	}
    	
        model.addAttribute("functions", functionService.getAll());

        return "admin/functionHierarchy/functions/list";
    }
    
    @GetMapping("/ui/admin/functionhierarchy/facets")
    public String listFacets(Model model) {
    	if (!config.getModules().getFunctionHierarchy().isEnabled()) {
    		return "error";
    	}
    	
        model.addAttribute("facets", facetService.getAll());

        return "admin/functionHierarchy/facets/list";
    }
    
    @GetMapping("/ui/admin/functionhierarchy/notifications")
    public String notifications(Model model) {
    	if (!config.getModules().getFunctionHierarchy().isEnabled()) {
    		return "error";
    	}
    	
    	model.addAttribute("functionAssignmentEmployeeNewManager", settingService.getFunctionAssignmentEmployeeNewManager());
    	model.addAttribute("functionAssignmentExpires", settingService.getFunctionAssignmentExpires());
    	model.addAttribute("daysBeforeFunctionAssignmentExpires", settingService.getDaysBeforeFunctionAssignmentExpires());

        return "admin/functionHierarchy/notifications";
    }
    
    @GetMapping("/ui/admin/functionhierarchy/functions/new")
    public String createFunction(Model model) {
    	if (!config.getModules().getFunctionHierarchy().isEnabled()) {
    		return "error";
    	}
    	
        model.addAttribute("form", new FunctionDTO());
        model.addAttribute("facets", facetService.getAll());

        return "admin/functionHierarchy/functions/update";
    }
    
    @GetMapping("/ui/admin/functionhierarchy/functions/{id}")
    public String editFunction(Model model, @PathVariable long id) {
    	Function function = functionService.getById(id);
    	if (!config.getModules().getFunctionHierarchy().isEnabled() || function == null) {
    		return "error";
    	}
    	
    	FunctionDTO form = new FunctionDTO();
    	form.setId(id);
    	form.setDescription(function.getDescription());
    	form.setName(function.getName());
    	form.setFacetIds(StringUtils.join(function.getFacets().stream().map(f -> f.getId()).collect(Collectors.toList()), ","));
    	
        model.addAttribute("form", form);
        model.addAttribute("facets", facetService.getAll());
        model.addAttribute("edit", true);

        return "admin/functionHierarchy/functions/update";
    }
    
    @PostMapping("/ui/admin/functionhierarchy/functions/update")
	public String updateFunctionPost(Model model, @Valid @ModelAttribute FunctionDTO functionDTO, BindingResult bindingResult) {
    	if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("form", functionDTO);

			return "admin/functionHierarchy/functions/update";
		}
    	
    	Function function = functionService.getById(functionDTO.getId());
    	if (function == null) {
    		function = new Function();

    		List<Function> functions = functionService.getAll();
    		if (functions.size() > 0) {
        		Comparator<Function> compareBySortKey = (Function o1, Function o2) -> Integer.compare(o1.getSortKey(), o2.getSortKey());
        		Collections.sort(functions, compareBySortKey);

    			function.setSortKey(functions.get(functions.size() - 1).getSortKey() + 1);
    		}
    		else {
    			function.setSortKey(1);
    		}
    	}
    	
		function.setName(functionDTO.getName());
		function.setDescription(functionDTO.getDescription());
		
		List<Facet> facets = new ArrayList<>();
		if (functionDTO.getFacetIds() != null && !functionDTO.getFacetIds().trim().isEmpty()) {
			for (String facetId : Arrays.asList(functionDTO.getFacetIds().split(","))) {
				Facet facet = facetService.getById(Long.parseLong(facetId));
				if (facet == null) {
					log.warn("Tried to add facet with id " + facetId + " to new function, but facet does not exist.");
					continue;
				}
				facets.add(facet);
			}
		}
		
		if (function.getFacets() == null) {
			function.setFacets(facets);
		} else {
			function.getFacets().clear();
			function.getFacets().addAll(facets);
		}
		
		functionService.save(function);

		return "redirect:/ui/admin/functionhierarchy/functions";
	}
    
    @GetMapping("/ui/admin/functionhierarchy/facets/new")
    public String createFacet(Model model) {
    	if (!config.getModules().getFunctionHierarchy().isEnabled()) {
    		return "error";
    	}
    	
        model.addAttribute("form", new FacetDTO());
        model.addAttribute("facetTypes", FacetType.values());

        return "admin/functionHierarchy/facets/update";
    }
    
    @GetMapping("/ui/admin/functionhierarchy/facets/{id}")
    public String createFacet(Model model, @PathVariable long id) {
    	Facet facet = facetService.getById(id);
    	if (!config.getModules().getFunctionHierarchy().isEnabled() || facet == null) {
    		return "error";
    	}
    	
    	FacetDTO form = new FacetDTO();
    	form.setId(facet.getId());
    	form.setName(facet.getName());
    	form.setDescription(facet.getDescription());
    	form.setPattern(facet.getPattern());
    	form.setType(facet.getType());
    	form.setListItems(StringUtils.join(facet.getListItems().stream().map(l -> l.getText()).collect(Collectors.toList()), ","));
    	
        model.addAttribute("form", form);
        model.addAttribute("facetTypes", FacetType.values());
        model.addAttribute("edit", true);

        return "admin/functionHierarchy/facets/update";
    }
    
    @PostMapping("/ui/admin/functionhierarchy/facets/update")
	public String updateFacetPost(Model model, @Valid @ModelAttribute FacetDTO facetDTO, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("form", facetDTO);
			model.addAttribute("facetTypes", FacetType.values());

			return "admin/functionHierarchy/facets/update";
		}
		
		if (facetDTO.getType().equals(FacetType.LIST) && facetDTO.getListItems().trim().isEmpty()) {
			model.addAttribute("form", facetDTO);
			model.addAttribute("facetTypes", FacetType.values());
			model.addAttribute("listItemEmptyError", "Når typen valgliste er valgt, skal der oprettes valgmuligheder.");
			
			return "admin/functionHierarchy/facets/update";
		}
		
		Facet facet = facetService.getById(facetDTO.getId());
		if (facet == null) {
			facet = new Facet();
			facet.setType(facetDTO.getType());
		}
		
		facet.setName(facetDTO.getName());
		facet.setDescription(facetDTO.getDescription());
		
		if (facetDTO.getType().equals(FacetType.LIST)) {
			if (facet.getListItems() == null) {
				facet.setListItems(new ArrayList<>());
			}
			
			List<String> listItemTexts = Arrays.asList(facetDTO.getListItems().split(","));
			
			List<String> existingFacetListItemTexts = facet.getListItems().stream().map(l -> l.getText()).collect(Collectors.toList());
			for (String listItem : listItemTexts) {
				FacetListItem facetListItem = new FacetListItem();
				facetListItem.setFacet(facet);
				facetListItem.setText(listItem);
				
				if (!existingFacetListItemTexts.contains(listItem)) {
					facet.getListItems().add(facetListItem);
				}
			}
			
			List<FacetListItem> toDelete = facet.getListItems().stream().filter(l -> !listItemTexts.contains(l.getText())).collect(Collectors.toList());
			facet.getListItems().removeAll(toDelete);
			
		} else if (facetDTO.getType().equals(FacetType.FREETEXT)) {
			facet.setPattern(facetDTO.getPattern());
		}
		
		facetService.save(facet);

		return "redirect:/ui/admin/functionhierarchy/facets";
	}
}
