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
package com.turkerozturk.dtt.controller.web;


import com.turkerozturk.dtt.helper.EntityIcons;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.service.CategoryService;
import com.turkerozturk.dtt.service.CategoryProfileService;
import com.turkerozturk.dtt.service.CategoryListService;

import java.util.Optional;
import java.text.Collator;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.context.i18n.LocaleContextHolder;

@Controller
@RequestMapping("/categories")
public class CategoryWebController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryGroupRepository categoryGroupRepository;

    private final CategoryListService categoryListService;

    private final CategoryProfileService profileService;

    public CategoryWebController(CategoryListService categoryListService, CategoryProfileService profileService) {
        this.categoryListService = categoryListService;
        this.profileService = profileService;
    }

    @GetMapping
    public String listCategories(@ModelAttribute("filter") CategoryListService.Filter filter,
                                 HttpServletRequest request, HttpServletResponse response, Model model) {
        var groups = categoryGroupRepository.findAll();
        groups.sort(Comparator.comparing(CategoryGroup::getName, Collator.getInstance(LocaleContextHolder.getLocale())));
        var activeProfile = profileService.getActiveProfile().orElse(null);
        if (activeProfile != null) {
            var groupIds = activeProfile.getCategories().stream()
                    .map(category -> category.getCategoryGroup().getId()).collect(Collectors.toSet());
            groups = groups.stream().filter(group -> groupIds.contains(group.getId())).toList();
            model.addAttribute("activeCategoryProfileName", activeProfile.getName());
            model.addAttribute("groupOutsideProfile", filter.groupId() != null && !groupIds.contains(filter.groupId()));
        }
        var categories = categoryListService.browse(filter, activeProfile == null ? null : activeProfile.getId());
        String listReturn = filter.path(categories.getNumber());
        model.addAttribute("groups", groups);
        model.addAttribute("categoriesPage", categories);
        model.addAttribute("sortOptions", CategoryListService.Sort.values());
        model.addAttribute("listReturn", listReturn);
        model.addAttribute("previousPath", categories.hasPrevious() ? filter.path(categories.getNumber() - 1) : null);
        model.addAttribute("nextPath", categories.hasNext() ? filter.path(categories.getNumber() + 1) : null);

        response.addHeader("Vary", "HX-Request, HX-History-Restore-Request");
        if ("true".equals(request.getHeader("HX-Request"))
                && !"true".equals(request.getHeader("HX-History-Restore-Request"))) {
            response.setHeader("HX-Push-Url", request.getContextPath() + listReturn);
            return "categories/category-list :: results";
        }
        return "categories/category-list";
    }

    // Yeni kategori oluşturma formunu getiren endpoint
    @GetMapping("/create")
    public String showCreateForm(Model model,
                                 @RequestParam(name="returnPage", required=false) String returnPage,
                                 @RequestParam(name="categoryGroupId", required=false) Long categoryGroupId,
                                 @RequestParam(required = false) String listReturn) {
        Category category = new Category();
        if(categoryGroupId != null) {
            CategoryGroup categoryGroup = categoryGroupRepository.findById(categoryGroupId).get();
            category.setCategoryGroup(categoryGroup);
        }
        model.addAttribute("categoryDTO", category);
        model.addAttribute("allGroups", categoryGroupRepository.findAllByOrderByIdDesc());
        model.addAttribute("returnPage", returnPage);

        addListReturn(model, listReturn, null);
        return "categories/category-form"; // templates/category-form.html
    }

    // Yeni kategori kaydetmek için (form post)
    @PostMapping
    public String saveCategory(@ModelAttribute("categoryDTO") Category category,
                               @RequestParam(required = false) String iconKey,
                               @RequestParam(required = false) String listReturn,
                               RedirectAttributes redirectAttrs,
                               HttpServletRequest request,
                               Model model) {
        String listPath = PageReturnPath.categoryList(listReturn);
        category.setIconKey(EntityIcons.selection(null, iconKey));
        categoryService.saveCategory(category);
        redirectAttrs.addFlashAttribute("success", "Category created successfully.");
        if (listPath != null) {
            return "redirect:" + PageReturnPath.categoryAnchor(listPath, category.getId());
        }

        String returnPage = request.getParameter("returnPage");

        if (returnPage != null && !returnPage.isBlank()) {

            return "redirect:/entries/redirect"
                + "?returnPage=" + returnPage;
               // + (categoryId != null ? "&categoryId=" + categoryId : "")
               // + (topicId != null ? "&topicId=" + topicId : "");
        }
        return "redirect:/categories";
    }

    // Mevcut kategoriyi düzenleme formu
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
                               @RequestParam(required = false) String listReturn,
                               @RequestParam(required = false) String returnPage
                               ) {
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        model.addAttribute("allGroups", categoryGroupRepository.findAllByOrderByIdDesc());
        model.addAttribute("categoryDTO", category);
        model.addAttribute("returnPage", returnPage);


        addListReturn(model, listReturn, id);
        return "categories/category-form"; // Aynı formu kullanacağız
    }

    // Mevcut kategoriyi güncellemek için (form post)
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @ModelAttribute("categoryDTO") Category dto,
                                 @RequestParam(required = false) String iconKey,
                                 @RequestParam(required = false) String returnPage,
                                 @RequestParam(required = false) String listReturn,
                                 RedirectAttributes redirectAttributes) {
        String listPath = PageReturnPath.categoryList(listReturn);
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        // Sadece adını güncellesin diye basit bir şekilde yazıyoruz
        category.setIconKey(EntityIcons.selection(category.getIconKey(), iconKey));
        category.setName(dto.getName());
        category.setArchived(dto.isArchived());
       // category.setCategoryGroupNumber(dto.getCategoryGroupNumber());

        category.setCategoryGroup(dto.getCategoryGroup());

        // Topics liste gibi daha fazlası varsa burada set edebilirsiniz.
        categoryService.saveCategory(category);
        if (listPath != null) {
            redirectAttributes.addFlashAttribute("success", "Category updated.");
            return "redirect:" + PageReturnPath.categoryAnchor(listPath, id);
        }


        Long categoryId = category.getId();

        if (returnPage != null && !returnPage.isBlank()) {

            return "redirect:/entries/redirect"
                    + "?returnPage=" + returnPage
             + (categoryId != null ? "&categoryId=" + categoryId : "");
            // + (topicId != null ? "&topicId=" + topicId : "");
        }

        return "redirect:/categories";
    }

    private void addListReturn(Model model, String listReturn, Long categoryId) {
        String listPath = PageReturnPath.categoryList(listReturn);
        if (listPath != null) {
            model.addAttribute("listReturn", listPath);
            model.addAttribute("categoryListPath", categoryId == null ? listPath : PageReturnPath.categoryAnchor(listPath, categoryId));
        }
    }

    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteCategory(@PathVariable Long id, @RequestParam(required = false) String listReturn,
                                 RedirectAttributes redirectAttributes) {
        String listPath = PageReturnPath.categoryList(listReturn);
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (listPath != null ? listPath : "/categories");
    }

    @GetMapping("/info/{id}")
    public String info(@PathVariable Long id, Model model) {
        Optional<Category> optionalCategory = categoryService.getCategoryById(id);
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();

            model.addAttribute("category", category);
            return "categories/category-info";
        } else {
            // Eğer kategori bulunamazsa bir hata sayfasına veya yönlendirmeye gidebilirsiniz
            return "redirect:/categories?error=notfound";
        }
    }

}
