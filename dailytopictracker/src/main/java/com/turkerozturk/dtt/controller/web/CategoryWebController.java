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


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.service.CategoryService;

import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/categories")
public class CategoryWebController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryGroupRepository categoryGroupRepository;

    // Tüm kategorileri listeleyen sayfa
    @GetMapping
    public String listCategories(Model model) {
        var categoryDTOs = categoryService.getAllCategories()
                .stream()

                .collect(Collectors.toList());

        model.addAttribute("categories", categoryDTOs);
        return "categories/category-list";
    }

    // Yeni kategori oluşturma formunu getiren endpoint
    @GetMapping("/create")
    public String showCreateForm(Model model,
                                 @RequestParam(name="returnPage", required=false) String returnPage,
                                 @RequestParam(name="categoryGroupId", required=false) Long categoryGroupId) {
        Category category = new Category();
        if(categoryGroupId != null) {
            CategoryGroup categoryGroup = categoryGroupRepository.findById(categoryGroupId).get();
            category.setCategoryGroup(categoryGroup);
        }
        model.addAttribute("categoryDTO", category);
        model.addAttribute("allGroups", categoryGroupRepository.findAllByOrderByIdDesc());
        model.addAttribute("returnPage", returnPage);

        return "categories/category-form"; // templates/category-form.html
    }

    // Yeni kategori kaydetmek için (form post)
    @PostMapping
    public String saveCategory(@ModelAttribute("categoryDTO") Category category,
                               RedirectAttributes redirectAttrs,
                               HttpServletRequest request,
                               Model model) {
        categoryService.saveCategory(category);
        redirectAttrs.addFlashAttribute("success", "Category created successfully.");

        String returnPage = request.getParameter("returnPage");

        if (returnPage != null) {

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
                               @ModelAttribute("returnPage") String returnPage
                               ) {
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        model.addAttribute("allGroups", categoryGroupRepository.findAllByOrderByIdDesc());
        model.addAttribute("categoryDTO", category);
        model.addAttribute("returnPage", returnPage);


        return "categories/category-form"; // Aynı formu kullanacağız
    }

    // Mevcut kategoriyi güncellemek için (form post)
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @ModelAttribute("categoryDTO") Category dto,
                                 @ModelAttribute("returnPage") String returnPage) {
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        // Sadece adını güncellesin diye basit bir şekilde yazıyoruz
        category.setName(dto.getName());
        category.setArchived(dto.isArchived());
       // category.setCategoryGroupNumber(dto.getCategoryGroupNumber());

        category.setCategoryGroup(dto.getCategoryGroup());

        // Topics liste gibi daha fazlası varsa burada set edebilirsiniz.
        categoryService.saveCategory(category);

        Long categoryId = category.getId();

        if (returnPage != null) {

            return "redirect:/entries/redirect"
                    + "?returnPage=" + returnPage
             + (categoryId != null ? "&categoryId=" + categoryId : "");
            // + (topicId != null ? "&topicId=" + topicId : "");
        }

        return "redirect:/categories";
    }

    // Kategori silme
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/categories";
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
