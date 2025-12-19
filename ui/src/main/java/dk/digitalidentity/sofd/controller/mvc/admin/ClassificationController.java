package dk.digitalidentity.sofd.controller.mvc.admin;

import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.classification.ClassificationService;
import dk.digitalidentity.sofd.service.classification.model.ClassificationDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationWithItemsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/ui/classifications")
@RequiredArgsConstructor
public class ClassificationController {

    private final ClassificationService classificationService;

    @RequireReadAccess
    @GetMapping
    public String listClassifications(Model model) {
        List<ClassificationDTO> classifications = classificationService.getAllClassifications();
        model.addAttribute("classifications", classifications);
        return "admin/classification/list";
    }

    @RequireReadAccess
    @GetMapping("/{identifier}")
    public String viewClassification(@PathVariable String identifier, Model model) {
        try {
            ClassificationWithItemsDTO classification = classificationService.getClassificationByIdentifier(identifier);
            model.addAttribute("classification", classification);
            return "admin/classification/view";
        } catch (IllegalArgumentException ex) {
            return "redirect:/ui/classifications?error=notfound";
        }
    }
}