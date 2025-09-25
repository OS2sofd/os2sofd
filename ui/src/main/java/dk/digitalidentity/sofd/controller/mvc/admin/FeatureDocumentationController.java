package dk.digitalidentity.sofd.controller.mvc.admin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireAdminAccess
@Controller
public class FeatureDocumentationController {

	@Autowired
	private SofdConfiguration configuration;
	
	record FeatureDTO (String name, String description, boolean enabled) {}
	@GetMapping(value = "/ui/admin/featuredocumentation")
	public String getFeatureDocumentation(Model model) {
		
		List<FeatureDTO> features = new ArrayList<>();
		getFields(configuration.getClass().getDeclaredFields(), features, configuration);
		
		model.addAttribute("features", features);
	    
		return "feature_documentation/list";
	}

	private void getFields(Field[] fields, List<FeatureDTO> features, Object object) {
	    try {
	        for (Field field : fields) {
	            field.setAccessible(true);

	            if (field.getType() instanceof Class && ((Class<?>)field.getType()).isEnum()) {
	            	; // ignore enums
	            }
	            else if (field.isAnnotationPresent(FeatureDocumentation.class)) {
					FeatureDocumentation annotation = field.getAnnotation(FeatureDocumentation.class);
					if( field.getType().equals(boolean.class) ) {
						FeatureDTO feature = new FeatureDTO(annotation.name(), annotation.description(), field.getBoolean(object));
						features.add(feature);
					}
					else if(field.getType().equals(String.class)) {
						FeatureDTO feature = new FeatureDTO(annotation.name(), annotation.description(), field.get(object) != null);
						features.add(feature);
					}
			    }
	            else {
			    	if (field.getType().getPackageName().startsWith("dk.")) {
			    		getFields(field.getType().getDeclaredFields(), features, field.get(object));
			    	}
			    }
	        }
	    }
	    catch (IllegalArgumentException e) {
	        log.error("A method has been passed a wrong argument in the getFields method for feature documentation.");
	    }
	    catch (SecurityException e) {
	    	log.error("Security violation in the getFields method for feature documentation");
		}
	    catch (IllegalAccessException e) {
			log.error("tries to acces a field or method, that is not allowed from the getFields method for feature documentation");
		}
	}
}
