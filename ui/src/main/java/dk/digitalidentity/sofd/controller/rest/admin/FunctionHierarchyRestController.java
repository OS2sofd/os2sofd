package dk.digitalidentity.sofd.controller.rest.admin;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Facet;
import dk.digitalidentity.sofd.dao.model.Function;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.FacetService;
import dk.digitalidentity.sofd.service.FunctionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireAdminAccess
@RestController
public class FunctionHierarchyRestController {
	
	@Autowired
    private FunctionService functionService;
	
	@Autowired
    private FacetService facetService;
		
	@DeleteMapping("/rest/functionhierarchy/functions/{id}/delete")
	public ResponseEntity<String> deleteFunction(@PathVariable long id) {
		Function function = functionService.getById(id);
		if (function == null) {
			log.warn("Requested Function with ID:" + id + " not found.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		functionService.delete(function);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@DeleteMapping("/rest/functionhierarchy/facets/{id}/delete")
	public ResponseEntity<String> deleteFacet(@PathVariable long id) {
		Facet facet = facetService.getById(id);
		if (facet == null) {
			log.warn("Requested facet with ID:" + id + " not found.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		facetService.delete(facet);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/rest/functionhierarchy/functions/{id}/sort")
	public ResponseEntity<String> deleteFunction(@PathVariable long id, @RequestParam String action) {
		Function function = functionService.getById(id);
		if (function == null) {
			log.warn("Requested Function with ID:" + id + " not found.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		Comparator<Function> compareBySortKey = (Function o1, Function o2) -> Integer.compare(o1.getSortKey(), o2.getSortKey());
		List<Function> functions = functionService.getAll();
		Collections.sort(functions, compareBySortKey);

		int oldSortKey = function.getSortKey();
		int index = functions.indexOf(function);
		
		if ("up".equals(action)) {
			if (index == 0) {
				return new ResponseEntity<>(HttpStatus.OK);
			}
			
			Function switchFunction = functions.get(index - 1);
			int switchFunctionSortKey = switchFunction.getSortKey();
			switchFunction.setSortKey(oldSortKey);
			function.setSortKey(switchFunctionSortKey);
			
			functionService.save(function);
			functionService.save(switchFunction);
			
		} else if ("down".equals(action)) {
			if (index == functions.size() - 1) {
				return new ResponseEntity<>(HttpStatus.OK);
			}
			
			Function switchFunction = functions.get(index + 1);
			int switchFunctionSortKey = switchFunction.getSortKey();
			switchFunction.setSortKey(oldSortKey);
			function.setSortKey(switchFunctionSortKey);
			
			functionService.save(function);
			functionService.save(switchFunction);
		} else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
