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
package turkerozturk.ptt.service;

import org.springframework.context.i18n.LocaleContextHolder;
import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;



    public List<Category> getAllCategories() {
        // Veritabanından tüm kategorileri çekiyoruz.
        List<Category> categories = categoryRepository.findAll();

        // Mevcut locale'i session'dan alıyoruz.
        Locale currentLocale = LocaleContextHolder.getLocale();

        // Locale'e uygun bir Collator örneği oluşturuyoruz.
        Collator collator = Collator.getInstance(currentLocale);

        // Category.getName() metodu ile kategori adını alarak sıralama yapıyoruz.
        categories.sort((c1, c2) -> collator.compare(c1.getName(), c2.getName()));

        return categories;
    }
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            if (category.getTopics() != null && !category.getTopics().isEmpty()) {
                // Bağlı topic'ler varsa silme!
                throw new IllegalStateException("Cannot delete category with associated topics.");
            }
            categoryRepository.delete(category);
        }
    }

}