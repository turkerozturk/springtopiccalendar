package com.turkerozturk.dtt.controller.export;


import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.text.Collator;
import java.util.List;
import java.util.Locale;


@Controller
@RequiredArgsConstructor
public class ExportPageController {

    private final CategoryGroupRepository categoryGroupRepository;

    @GetMapping("/export/category-groups-page")
    public String showExportPage(Model model) {

        // Mevcut locale'i session'dan alıyoruz.
        Locale currentLocale = LocaleContextHolder.getLocale();

        // Locale'e uygun bir Collator örneği oluşturuyoruz.
        Collator collator = Collator.getInstance(currentLocale);
        collator.setStrength(Collator.PRIMARY);

        List<CategoryGroup> categoryGroups = categoryGroupRepository.findAll();
        categoryGroups.sort((c1, c2) -> collator.compare(c1.getName(), c2.getName()));



        model.addAttribute("categoryGroups", categoryGroups);



        return "export/category-groups";
    }
}
