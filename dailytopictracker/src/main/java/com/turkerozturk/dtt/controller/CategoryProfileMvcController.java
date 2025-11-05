package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.dto.CategoryProfileForm;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.entity.CategoryProfile;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.repository.CategoryProfileRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.service.CategoryProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.Collator;
import java.util.*;

@Controller
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class CategoryProfileMvcController {

    private final CategoryProfileRepository categoryProfileRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryProfileService categoryProfileService;
    private final CategoryGroupRepository  categoryGroupRepository;

    @GetMapping
    public String listProfiles(Model model) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        Collator collator = Collator.getInstance(currentLocale);

        List<CategoryProfile> profiles = categoryProfileRepository.findAll();

        profiles.forEach(p -> {
            List<Category> sortedCategories = new ArrayList<>(p.getCategories());
            sortedCategories.sort(Comparator.comparing(Category::getName, collator));
            // geçici olarak modele konulacak property
            p.setCategoriesSorted(sortedCategories);
        });

        model.addAttribute("profiles", profiles);
        model.addAttribute("activeProfileId", categoryProfileService.getActiveProfileId().orElse(null));
        return "profiles/list";
    }




    @GetMapping("/new")
    public String newProfile(Model model) {
        model.addAttribute("profileForm", new CategoryProfileForm());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        return "profiles/form";
    }

    @GetMapping("/{id}/edit")
    public String editProfile(@PathVariable Long id, Model model) {
        CategoryProfile profile = categoryProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        CategoryProfileForm form = new CategoryProfileForm();
        form.setId(profile.getId());
        form.setName(profile.getName());
        form.setSelectedCategoryIds(profile.getCategories().stream()
                .map(Category::getId).toList());

        Locale currentLocale = LocaleContextHolder.getLocale();
        Collator collator = Collator.getInstance(currentLocale);

        // Kategorileri gruplayalım
        List<CategoryGroup> groups = categoryGroupRepository.findAll();
        Map<CategoryGroup, List<Category>> groupedCategories = new LinkedHashMap<>();

        groups.sort(Comparator.comparing(CategoryGroup::getName, collator));

        for (CategoryGroup group : groups) {
            List<Category> cats = new ArrayList<>(group.getCategories());
            cats.sort(Comparator.comparing(Category::getName, collator));
            groupedCategories.put(group, cats);
        }

        model.addAttribute("profileForm", form);
        model.addAttribute("groupedCategories", groupedCategories);
        return "profiles/form";
    }

    @PostMapping("/save")
    public String saveProfile(@ModelAttribute("profileForm") CategoryProfileForm form) {
        CategoryProfile profile = (form.getId() != null)
                ? categoryProfileRepository.findById(form.getId())
                .orElse(new CategoryProfile())
                : new CategoryProfile();

        profile.setName(form.getName());

        List<Category> selectedCats = categoryRepository.findAllById(form.getSelectedCategoryIds());
        profile.setCategories(new HashSet<>(selectedCats));

        categoryProfileRepository.save(profile);

        return "redirect:/profiles";
    }

    @PostMapping("/{id}/delete")
    public String deleteProfile(@PathVariable Long id) {
        categoryProfileRepository.deleteById(id);
        return "redirect:/profiles";
    }

    @PostMapping("/{id}/activate")
    public String activateProfile(@PathVariable Long id) {
        categoryProfileService.activateProfile(id);
        return "redirect:/profiles";
    }

    @PostMapping("/unarchive-all")
    public String unarchiveAll() {
        categoryProfileService.resetAllCategoriesUnarchived();
        return "redirect:/profiles";
    }

}

