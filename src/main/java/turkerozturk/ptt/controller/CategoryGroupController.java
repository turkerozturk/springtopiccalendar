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
package turkerozturk.ptt.controller;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import turkerozturk.ptt.entity.CategoryGroup;
import turkerozturk.ptt.repository.CategoryGroupRepository;

@Controller
@RequestMapping("/category-groups")
public class CategoryGroupController {

    private final CategoryGroupRepository repo;

    public CategoryGroupController(CategoryGroupRepository repo) {
        this.repo = repo;
    }

    // LIST
    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", repo.findAll());
        return "category-groups/list";
    }

    // SHOW CREATE FORM
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("group", new CategoryGroup());
        return "category-groups/form";
    }

    // SHOW EDIT FORM
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryGroup grp = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid id:"+id));
        model.addAttribute("group", grp);
        return "category-groups/form";
    }

    // SAVE OR UPDATE
    @PostMapping("/save")
    public String save(
            @ModelAttribute("group") CategoryGroup group,
            RedirectAttributes redirectAttrs
    ) {
        if (repo.existsByName(group.getName())) {
            redirectAttrs.addFlashAttribute("error", "Cannot create duplicate category group.");
        } else {
            repo.save(group);
            redirectAttrs.addFlashAttribute("success", "Category group saved successfully.");
        }

        return "redirect:/category-groups";
    }

    // DELETE
    @GetMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttrs
    ) {
        CategoryGroup group = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid CategoryGroup id: " + id));

        if (!group.getCategories().isEmpty()) {
            redirectAttrs.addFlashAttribute(
                    "error",
                    "Delete operation cancelled: This category group has categories."
            );
            return "redirect:/category-groups";
        }

        // no children → safe to delete
        repo.delete(group);
        redirectAttrs.addFlashAttribute(
                "success",
                "Successfully deleted the category group."
        );
        return "redirect:/category-groups";
    }
}

