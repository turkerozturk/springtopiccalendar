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

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.component.ParserRegistryLoader;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.CategoryService;
import com.turkerozturk.dtt.service.TopicService;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/topics")
public class TopicWebController {

    @Autowired
    private TopicService topicService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ParserRegistryLoader parserRegistryLoader;

    private final AppTimeZoneProvider timeZoneProvider;

    public TopicWebController(AppTimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }


    // Tüm topic'leri veya belli kategoriye ait topic'leri listeleyen sayfa
    @GetMapping
    public String listTopics(@RequestParam(value = "categoryId", required = false) Long categoryId,
                             Model model) {
        // Tüm kategorileri drop-down'da göstermek için çekelim
        var categoryList = categoryService.getAllCategories();

        // Eğer kategori ID geldiyse filtreli liste, yoksa tüm liste
        List<Topic> topicList;
        if (categoryId != null) {
            topicList = topicService.getTopicsByCategoryId(categoryId);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            topicList = topicService.getAllTopics();
            // Seçili kategori yoksa null bırakabiliriz
            model.addAttribute("selectedCategoryId", null);
        }

        // Modele eklemeler
        model.addAttribute("topics", topicList);
        model.addAttribute("categories", categoryList);

        return "topics/topic-list"; // templates/topics.html
    }



    // Yeni topic oluşturma formu
    @GetMapping("/create")
    public String showCreateForm(HttpSession session,
                                 @RequestParam(name="categoryId", required=false) Long categoryId,
                                 @RequestParam(name="returnPage", required=false) String returnPage,
                                 Model model) {


        // Yeni boş Topic nesnesi (formda doldurulacak)
        Topic topic = new Topic();

        // Eğer bir categoryId gelmişse, bu ID'ye karşılık gelen Category'yi bulup topic’e set ediyoruz
        if (categoryId != null) {
            Category cat = categoryService.getCategoryById(categoryId).orElse(null);

                topic.setCategory(cat);

        }



        // Boş bir DTO
        model.addAttribute("topic", topic);

        // Kategori seçimi için tüm kategorileri DTO olarak modele ekleyelim
        var categoryDTOList = categoryService.getAllCategories()
                .stream()
                .collect(Collectors.toList());
        model.addAttribute("categories", categoryDTOList);

        // pass returnPage back into the template
        model.addAttribute("returnPage", returnPage);

        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);

        model.addAttribute("availableParsers", parserRegistryLoader.getParserClassNames());

        return "topics/topic-form"; // templates/topic-form.html
    }

    // Yeni topic kaydetme
    @PostMapping
    public String saveTopic(@ModelAttribute("topic") Topic topic,
                            @RequestParam(name="returnPage", required=false) String returnPage
                            ) {
        // Seçilen category'yi veritabanından bul
        var categoryId = topic.getCategory().getId();
        Category category = categoryService
                .getCategoryById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found!"));

        // DTO -> Entity

        // Yukarıda category otomatik set ediyordu ama orada from() -> Category.from() diyordu;
        // projenizde o methodun nasıl çalıştığına bağlı olarak burada da set edebilirsiniz:
        topic.setCategory(category);

        // Kaydet
        topicService.saveTopic(topic);

        // Hangi sayfadan gelindiğini kontrol ediyoruz.
        if (returnPage != null) {
            switch (returnPage) {
                case "home":
                    return "redirect:/";
                case "topics":
                    //   return "redirect:/topics?categoryId=" + categoryId;
                case "pivottable":
                 //   return "redirect:/entry-filter/return?categoryId=" + categoryId;
                    return "redirect:/entry-filter/return";
                case "entries":
                    return "redirect:/entries?topicId=" + topic.getId();
                case "reporttable":
                    return "redirect:/reports/all";
                    // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
                    //   return "redirect:/" + returnPage + "?categoryId=" + categoryId;
            }
        }



        return "redirect:/topics?categoryId=" + categoryId;
    }

    // Mevcut topic'i düzenleme formu
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Topic topic = topicService
                .getTopicById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found!"));



        // Tüm kategorileri çekip modele ekle
        var categoryDTOList = categoryService.getAllCategories()
                .stream()
                .collect(Collectors.toList());
        model.addAttribute("categories", categoryDTOList);

        model.addAttribute("topic", topic);

        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        model.addAttribute("zoneId", zoneId);

        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);

        model.addAttribute("availableParsers", parserRegistryLoader.getParserClassNames());

        return "topics/topic-form"; // Aynı formu kullanacağız
    }

    // Mevcut topic'i güncelleme
    @PostMapping("/update/{id}")
    public String updateTopic(@PathVariable Long id,
                              @ModelAttribute("topic") Topic topic) {
        // Veritabanından topic bul
        Topic existingTopic = topicService
                .getTopicById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found!"));

        // Topic bilgilerini güncelle
        existingTopic.setName(topic.getName());
        existingTopic.setDescription(topic.getDescription());


        // Kategori güncelle
        var categoryId = topic.getCategory().getId();
        Category category = categoryService
                .getCategoryById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found!"));
        existingTopic.setCategory(category);

        existingTopic.setSomeTimeLater(topic.getSomeTimeLater());
        existingTopic.setPinned(topic.isPinned());
        existingTopic.setWeight(topic.getWeight());

        existingTopic.setBaseDateMillisYmd(topic.getBaseDateMillisYmd());
        existingTopic.setEndDateMillisYmd(topic.getEndDateMillisYmd());
        //System.out.println(topic.getBaseDateMillisYmd());
        //System.out.println(topic.getBaseDate());
        existingTopic.setImageFileName(topic.getImageFileName());
        existingTopic.setDataClassName(topic.getDataClassName());

        // If the topic is changed while updating an existing record, the variables of the old and new topic are recalculated.
        topicService.updateTopicStatus(existingTopic.getId());

        return "redirect:/topics?categoryId=" + categoryId;
    }

    // Topic silme
    @GetMapping("/delete/{id}")
    public String deleteTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Topic topic = topicService.getTopicById(id).get(); // for redirection after delete
        var categoryId = topic.getCategory().getId(); // for redirection after delete

        try {
            topicService.deleteTopic(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/topics?categoryId=" + categoryId;
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
        result.add("/images/default.png");

        File baseDir = new File(TOPIC_IMAGES_DIR);
        collectRecursive(baseDir, result, baseDir.getAbsolutePath());

        // necessary to remove eduplicates
        result = new ArrayList<>(new LinkedHashSet<>(result));

        System.out.println(result.size());

      //Collections.sort(result.subList(1, result.size())); // default.png dışındakileri alfabetik sırala

        for(String r : result) {
            System.out.println(r);
        }


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
