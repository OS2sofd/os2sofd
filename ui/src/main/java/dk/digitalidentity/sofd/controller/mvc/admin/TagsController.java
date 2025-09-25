package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.List;
import java.util.Objects;

import dk.digitalidentity.sofd.dao.model.enums.TagType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.TagDTO;
import dk.digitalidentity.sofd.dao.model.Tag;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.TagsService;

@RequireAdminAccess
@Controller
public class TagsController {

	@Autowired
	private TagsService tagsService;

	@GetMapping(value = "/ui/tags")
	public String listTags(Model model) {
		List<Tag> tagsList = tagsService.findAll();
		model.addAttribute("tags", tagsList);
		return "admin/tags/list";
	}

	@PostMapping(value = "/ui/tags")
	public ResponseEntity<?> editTag(@RequestBody TagDTO tagDTO) {
		if (tagDTO.getId() == 0) {
			// new
			if (!tagsService.existsByValue(tagDTO.getValue())) {
				tagsService.save(Tag.builder()
						.value(tagDTO.getValue())
						.description(tagDTO.getDescription())
						.customValueEnabled(tagDTO.isCustomValueEnabled())
						.customValueUnique(tagDTO.isCustomValueUnique())
						.customValueName(tagDTO.getCustomValueName())
						.customValueRegex(tagDTO.getCustomValueRegex())
						.tagType(TagType.DEFAULT)
						.build());
			} else {
				return ResponseEntity.badRequest().body("unique");
			}
		} else {
			// update
			Tag tagEntity = tagsService.findById(tagDTO.getId());
			if (tagEntity == null) {
				return ResponseEntity.badRequest().build();
			}

			// If we're changing tag
			if (!Objects.equals(tagEntity.getValue(), tagDTO.getValue())) {
				// Check if it's unique
				if (!tagsService.existsByValue(tagDTO.getValue())) {
					tagEntity.setValue(tagDTO.getValue());
				} else {
					return ResponseEntity.badRequest().body("unique");
				}
			}
			tagEntity.setDescription(tagDTO.getDescription());
			tagEntity.setCustomValueEnabled(tagDTO.isCustomValueEnabled());
			tagEntity.setCustomValueUnique(tagDTO.isCustomValueUnique());
			tagEntity.setCustomValueName(tagDTO.getCustomValueName());
			tagEntity.setCustomValueRegex(tagDTO.getCustomValueRegex());

			tagsService.save(tagEntity);
		}

		return ResponseEntity.ok("");
	}

	@DeleteMapping(value = "/ui/tags")
	public ResponseEntity<?> removeTag(@RequestBody Long id) {
		Tag tagEntity = tagsService.findById(id);
		if (tagEntity == null) {
			return ResponseEntity.badRequest().build();
		}

		tagsService.delete(tagEntity.getId());

		return ResponseEntity.ok("");
	}
}
