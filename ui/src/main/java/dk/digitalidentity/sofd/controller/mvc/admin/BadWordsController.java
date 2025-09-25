package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dk.digitalidentity.sofd.dao.model.BadWord;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.BadWordsService;

@RequireAdminAccess
@Controller
public class BadWordsController {

	@Autowired
	private BadWordsService badWordsService;

	@GetMapping(value = "/ui/badwords")
	public String settings(Model model) {
		List<String> badWordsList = badWordsService.findAll().stream().map(BadWord::getValue).collect(Collectors.toList());
		model.addAttribute("badWords", badWordsList);

		return "admin/badwords/list";
	}

	@PostMapping(value = "/ui/badwords")
	public ResponseEntity<?> addNewWord(@RequestBody String word) {
		word = word.replaceAll("[^a-zA-Z0-9]", "");
		if (word.length() < 3) {
			return ResponseEntity.badRequest().build();
		}

		BadWord badWord = badWordsService.findBadWord(word);
		if (badWord != null) {
			return ResponseEntity.badRequest().build();
		}

		badWordsService.save(BadWord.builder().value(word).build());

		return ResponseEntity.ok("");
	}

	@DeleteMapping(value = "/ui/badwords")
	public ResponseEntity<?> removeNewWord(@RequestBody String word) {
		BadWord badWord = badWordsService.findBadWord(word);
		if (badWord == null) {
			return ResponseEntity.badRequest().build();
		}

		badWordsService.delete(badWord.getId());

		return ResponseEntity.ok("");
	}	
}
