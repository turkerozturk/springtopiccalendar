package com.turkerozturk.dtt.controller.export;


import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
public class ExportPageController {

    private final CategoryGroupRepository categoryGroupRepository;

    @GetMapping("/export/category-groups-page")
    public String showExportPage(Model model) {
        model.addAttribute("categoryGroups", categoryGroupRepository.findAll());
        return "export/category-groups";
    }
}
