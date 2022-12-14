package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.FunctionTypeDTO;
import dk.digitalidentity.sofd.service.FunctionTypeService;

@Controller
public class FunctionTypeController {

    @Autowired
    private FunctionTypeService functionTypeService;

    @GetMapping("/ui/functiontype")
    public String listFunctionTypes(Model model) {
        model.addAttribute("functionTypes", functionTypeService.findAllAsDTO());
        model.addAttribute("newFunctionType", new FunctionTypeDTO());

        return "admin/functionType/list";
    }
}
