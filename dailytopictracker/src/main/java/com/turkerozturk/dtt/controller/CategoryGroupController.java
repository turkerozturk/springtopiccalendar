/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.dtt.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.turkerozturk.dtt.dto.CategoryEntryStatsDto;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.service.CategoryService;

import java.io.File;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/category-groups")
public class CategoryGroupController {

    @Autowired
    private CategoryService categoryService;

    private final CategoryGroupRepository repo;

    //  satirlari dile gore dogru siralamak icin
    @Value("${app.locale:en}")
    private String appLocale;

    public CategoryGroupController(CategoryGroupRepository repo) {
        this.repo = repo;
    }

    // LIST
    @GetMapping
    public String list(Model model) {
        List<CategoryGroup> groups = repo.findAllByOrderByPriorityDesc();

        Locale locale = Locale.forLanguageTag(appLocale);
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);

        // DTO: Kategori ID'ye göre Map'e çevir
        List<CategoryEntryStatsDto> statsList = categoryService.getCategoryStats();
        Map<Long, CategoryEntryStatsDto> statsMap = statsList.stream()
                .collect(Collectors.toMap(CategoryEntryStatsDto::getCategoryId, Function.identity()));

        // Gruplar ve kategoriler
        groups.forEach(g -> {
            List<Category> sorted = g.getCategories().stream()
                    .peek(cat -> {
                        CategoryEntryStatsDto dto = statsMap.get(cat.getId());
                        if (dto != null) {
                            // sorgu sonucu dto ya yukledigimiz istatistik sayaclari
                            // Category entity'sinde karsilik gelen transient degiskenlere yukluyoruz.
                            cat.setWarningCount(dto.getWarningCount());
                            cat.setFutureNotMarked(dto.getFutureNotMarked());
                            cat.setTodayDone(dto.getTodayDone());
                            cat.setPredictionCount(dto.getPredictionCount());
                        }
                    })
                    .sorted(Comparator.comparing(Category::getName, collator))
                    .collect(Collectors.toList());
            g.setCategories(sorted);
        });

        model.addAttribute("groups", groups);
        return "category-groups/category-group-list";
    }


    // SHOW CREATE FORM
    @GetMapping("/new")
    public String createForm(Model model,
                             @RequestParam(name="returnPage", required=false) String returnPage) {
        CategoryGroup categoryGroup = new CategoryGroup();
        String defaultBgColor = "#f5f5f5"; // WhiteSmoke
        categoryGroup.setBackgroundColor(defaultBgColor);
        model.addAttribute("group", categoryGroup);
        model.addAttribute("returnPage", returnPage);
        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);


        return "category-groups/category-group-form";
    }

    // HANDLE CREATE
    @PostMapping("/new")
    public String create(
            @ModelAttribute("group") CategoryGroup formGroup,
            RedirectAttributes redirectAttrs,
            HttpServletRequest request,
            Model model
    ) {
        if (repo.existsByName(formGroup.getName())) {
            redirectAttrs.addFlashAttribute("error", "Cannot create duplicate category group.");
            return "redirect:/category-groups/new";
        }

        // Yeni CategoryGroup olusturulurken priority degeri otomatik ayarlanmali.
        // id daha oluşmadığı için onu doğrudan kullanamazsın.
        // Ama örneğin en yüksek mevcut priority + 1 olarak belirleyebilirsin.
        // Otomatik priority ataması
        Integer maxPriority = repo.findMaxPriority().orElse(0);
        formGroup.setPriority(maxPriority + 1);

        repo.save(formGroup);
        redirectAttrs.addFlashAttribute("success", "Category group created successfully.");

        String returnPage = request.getParameter("returnPage");

        if (returnPage != null) {
            switch (returnPage) {
                case "home":
                    return "redirect:/";
                // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
            }
        }
        return "redirect:/category-groups";
    }

    // SHOW EDIT FORM
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryGroup grp = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid id:"+id));
        model.addAttribute("group", grp);
        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);

        return "category-groups/category-group-form";
    }

    @InitBinder("group")
    public void initGroupBinder(WebDataBinder binder) {
        binder.setDisallowedFields("categories");
    }



    // HANDLE UPDATE
    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("group") CategoryGroup formGroup,
            RedirectAttributes redirectAttrs
    ) {
        if (repo.existsByName(formGroup.getName())
                && !repo.findByName(formGroup.getName()).get().getId().equals(id)) {
            // if a different group already uses that name
            redirectAttrs.addFlashAttribute("error", "Cannot create duplicate category group.");
            return "redirect:/category-groups/edit/" + id;
        }
        CategoryGroup managed = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid CategoryGroup id: " + id));
        managed.setName(formGroup.getName());
        managed.setBackgroundColor(formGroup.getBackgroundColor());
        managed.setImageFileName(formGroup.getImageFileName());
        repo.save(managed);
        redirectAttrs.addFlashAttribute("success", "Category group updated successfully.");
        return "redirect:/category-groups";
    }



    @Value("${can.delete.category.group.with.its.categories:false}")
    private boolean canDeleteCategoryGroupWithItsCategories;

    // DELETE
    @GetMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttrs
    ) {
        CategoryGroup group = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid CategoryGroup id: " + id));

        if (!group.getCategories().isEmpty()) {
            if(!canDeleteCategoryGroupWithItsCategories) {
                redirectAttrs.addFlashAttribute(
                        "error",
                        "Delete operation cancelled: This category group has categories."
                );
                return "redirect:/category-groups";
            }
        }

        // no children → safe to delete
        repo.delete(group);
        redirectAttrs.addFlashAttribute(
                "success",
                "Successfully deleted the category group."
        );
        return "redirect:/category-groups";
    }

    // codes below are related with topic images
    private static final String TOPIC_IMAGES_DIR = "topicimages";

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".svg");

    public void ensureTopicImagesDirectoryExists() {
        File dir = new File(TOPIC_IMAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public List<String> collectImagePaths() {
        ensureTopicImagesDirectoryExists();

        List<String> result = new ArrayList<>();
        result.add(""); // default secenek bos olmasi yani resim istemiyorum secenegi.
        //result.add("/images/default.png");

        File baseDir = new File(TOPIC_IMAGES_DIR);
        collectRecursive(baseDir, result, baseDir.getAbsolutePath());

        // necessary to remove eduplicates
        result = new ArrayList<>(new LinkedHashSet<>(result));

        //Collections.sort(result.subList(1, result.size())); // default.png dışındakileri alfabetik sırala

        return result;
    }

    private void collectRecursive(File currentDir, List<String> result, String basePath) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectRecursive(file, result, basePath);
            } else if (isSupportedImage(file.getName())) {
                String relativePath = file.getAbsolutePath().substring(basePath.length()).replace(File.separatorChar, '/');
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                result.add("/topicimages/" + relativePath);
            }
        }

    }

    private boolean isSupportedImage(String name) {
        String lower = name.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }


}

