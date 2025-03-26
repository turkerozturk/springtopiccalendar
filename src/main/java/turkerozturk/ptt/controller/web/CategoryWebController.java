package turkerozturk.ptt.controller.web;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.service.CategoryService;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/categories")
public class CategoryWebController {

    @Autowired
    private CategoryService categoryService;

    // Tüm kategorileri listeleyen sayfa
    @GetMapping
    public String listCategories(Model model) {
        var categoryDTOs = categoryService.getAllCategories()
                .stream()

                .collect(Collectors.toList());

        model.addAttribute("categories", categoryDTOs);
        return "categories"; // templates/categories.html
    }

    // Yeni kategori oluşturma formunu getiren endpoint
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryDTO", new Category());
        return "category-form"; // templates/category-form.html
    }

    // Yeni kategori kaydetmek için (form post)
    @PostMapping
    public String saveCategory(@ModelAttribute("categoryDTO") Category category) {
        categoryService.saveCategory(category);
        return "redirect:/categories";
    }

    // Mevcut kategoriyi düzenleme formu
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));




        model.addAttribute("categoryDTO", category);
        return "category-form"; // Aynı formu kullanacağız
    }

    // Mevcut kategoriyi güncellemek için (form post)
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @ModelAttribute("categoryDTO") Category dto) {
        Category category = categoryService
                .getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        // Sadece adını güncellesin diye basit bir şekilde yazıyoruz
        category.setName(dto.getName());
        // Topics liste gibi daha fazlası varsa burada set edebilirsiniz.
        categoryService.saveCategory(category);

        return "redirect:/categories";
    }

    // Kategori silme
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }
}
