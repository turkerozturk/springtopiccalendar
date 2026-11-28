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
import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.component.ParserRegistryLoader;
import com.turkerozturk.dtt.entity.Entry;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.CategoryService;
import com.turkerozturk.dtt.service.CategoryProfileService;
import com.turkerozturk.dtt.service.TopicDetailsService;
import com.turkerozturk.dtt.service.TopicListCriteria;
import com.turkerozturk.dtt.service.TopicListService;
import com.turkerozturk.dtt.service.TopicService;
import com.turkerozturk.dtt.service.TopicTagService;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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

    private final TopicDetailsService topicDetailsService;
    private final MarkdownService markdownService;
    private final TopicListService topicListService;
    private final CategoryProfileService profileService;
    private final TopicTagService topicTags;

    public TopicWebController(AppTimeZoneProvider timeZoneProvider, TopicDetailsService topicDetailsService,
                              MarkdownService markdownService, TopicListService topicListService,
                              CategoryProfileService profileService, TopicTagService topicTags) {
        this.timeZoneProvider = timeZoneProvider;
        this.topicDetailsService = topicDetailsService;
        this.markdownService = markdownService;
        this.topicListService = topicListService;
        this.profileService = profileService;
        this.topicTags = topicTags;
    }

    @GetMapping("/{id}")
    public String showTopic(@PathVariable long id, @RequestParam(required = false) Long listCategoryId,
                            @RequestParam(required = false) String listReturn,
                            @ModelAttribute("historyFilter") TopicHistoryFilter historyFilter, Model model) {
        var detail = topicDetailsService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("topic", detail);
        model.addAttribute("descriptionHtml", markdownService.render(detail.description()));
        long returnCategory = listCategoryId != null ? listCategoryId : detail.categoryId();
        String listPath = PageReturnPath.topicList(listReturn);
        model.addAttribute("listCategoryId", returnCategory);
        model.addAttribute("listReturn", listPath);
        model.addAttribute("topicListPath", PageReturnPath.topicAnchor(
                listPath != null ? listPath : "/topics?categoryId=" + returnCategory, id));
        String error = historyFilter.validationMessage();
        model.addAttribute("historyFilterError", error);
        if (error == null && historyFilter.isActive()) {
            model.addAttribute("entryHistory",
                    topicDetailsService.findHistory(id, historyFilter.fromDate(), historyFilter.toDate(),
                            historyFilter.pageIndex()));
        }
        return "topics/topic-detail";
    }


    @GetMapping
    public String listTopics(@RequestParam(required = false) Long categoryId,
                             @ModelAttribute("criteria") TopicListCriteria criteria,
                             HttpServletRequest request, HttpServletResponse response, Model model) {
        var categories = categoryService.getAllCategories();
        long selectedCategoryId = categoryId != null ? categoryId : TopicListService.ALL_CATEGORIES;
        var activeProfile = profileService.getActiveProfile().orElse(null);
        if (activeProfile != null) {
            Set<Long> categoryIds = activeProfile.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
            categories = categories.stream().filter(category -> categoryIds.contains(category.getId())).toList();
            model.addAttribute("activeCategoryProfileName", activeProfile.getName());
            model.addAttribute("categoryOutsideProfile",
                    selectedCategoryId != TopicListService.ALL_CATEGORIES && !categoryIds.contains(selectedCategoryId));
        }
        var topics = topicListService.browse(selectedCategoryId, criteria, activeProfile == null ? null : activeProfile.getId());
        String listReturn = criteria.path(selectedCategoryId, topics.getNumber());
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", selectedCategoryId);
        model.addAttribute("sortOptions", TopicListCriteria.Sort.values());
        model.addAttribute("topicsPage", topics);
        model.addAttribute("listReturn", listReturn);
        model.addAttribute("previousPath", topics.hasPrevious() ? criteria.path(selectedCategoryId, topics.getNumber() - 1) : null);
        model.addAttribute("nextPath", topics.hasNext() ? criteria.path(selectedCategoryId, topics.getNumber() + 1) : null);

        response.addHeader("Vary", "HX-Request, HX-History-Restore-Request");
        if ("true".equals(request.getHeader("HX-Request"))
                && !"true".equals(request.getHeader("HX-History-Restore-Request"))) {
            response.setHeader("HX-Push-Url", request.getContextPath() + listReturn);
            return "topics/topic-list :: results";
        }
        return "topics/topic-list";
    }

    // Yeni topic oluşturma formu
    @GetMapping("/create")
    public String showCreateForm(HttpSession session,
                                 @RequestParam(required = false) String listReturn,
                                 @RequestParam(required = false) String categoryReturn,
                                 @RequestParam(name="categoryId", required=false) Long categoryId,
                                 @RequestParam(name="returnPage", required=false) String returnPage,
                                 @RequestParam(name = "dateMillisYmd", required = false) Long dateMillisYmd,
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

        // basla
        List<Entry> entries = topic.getActivities();
        if(!entries.isEmpty()) {
            Long firstDoneEntryDateMillisYmd = entries.stream()
                    .filter(e -> e.getStatus() == 1) // 1 = done
                    .map(Entry::getDateMillisYmd)
                    .min(Long::compareTo)
                    .orElse(null);

            if (firstDoneEntryDateMillisYmd != null) {
                LocalDate firstDoneEntryDate = Instant
                        .ofEpochMilli(firstDoneEntryDateMillisYmd)
                        .atZone(AppTimeZoneProvider.getZone())
                        .toLocalDate();
                model.addAttribute("firstDoneEntryDateMillisYmd",firstDoneEntryDateMillisYmd);
                model.addAttribute("firstDoneEntryDate",firstDoneEntryDate);
            }
        }
        // bitti


        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        model.addAttribute("zoneId", zoneId);
        long todayMillisYmd = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli();
        model.addAttribute("todayMillisYmd", todayMillisYmd);

        // Kategori seçimi için tüm kategorileri DTO olarak modele ekleyelim
        var categoryDTOList = categoryService.getAllCategories()
                .stream()
                .collect(Collectors.toList());
        model.addAttribute("categories", categoryDTOList);

        // pass returnPage back into the template
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("dateMillisYmd", dateMillisYmd);

        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);

        model.addAttribute("availableParsers", parserRegistryLoader.getParserClassNames());

        String listPath = PageReturnPath.topicList(listReturn);
        if (listPath != null) {
            model.addAttribute("listReturn", listPath);
            model.addAttribute("topicListPath", topic.getId() == null ? listPath : PageReturnPath.topicAnchor(listPath, topic.getId()));
        }
        String categoryPath = PageReturnPath.categoryList(categoryReturn);
        if (categoryPath != null) {
            model.addAttribute("categoryReturn", categoryPath);
            model.addAttribute("topicListPath", categoryId == null ? categoryPath : PageReturnPath.categoryAnchor(categoryPath, categoryId));
        }
        return "topics/topic-form"; // templates/topic-form.html
    }

    // Yeni topic kaydetme
    @PostMapping
    @Transactional
    public String saveTopic(@ModelAttribute("topic") Topic topic,
                            @RequestParam(required = false) String tagLines,
                            @RequestParam(required = false) String listReturn,
                            @RequestParam(required = false) String categoryReturn,
                            @RequestParam(name="returnPage", required=false) String returnPage,
                            @RequestParam(required = false) Long dateMillisYmd
                            ) {
        String listPath = PageReturnPath.topicList(listReturn);
        String categoryPath = PageReturnPath.categoryList(categoryReturn);
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
        Topic saved = topicService.saveTopic(topic);
        topicTags.addTags(saved.getId(), topicTags.parseTags(tagLines));
        if (categoryPath != null) {
            return "redirect:" + PageReturnPath.categoryAnchor(categoryPath, categoryId);
        }

        if (listPath != null) {
            return "redirect:" + PageReturnPath.topicAnchor(listPath, topic.getId());
        }


        // Hangi sayfadan gelindiğini kontrol ediyoruz.
        if (returnPage != null) {
            switch (returnPage) {
                case "home":
                    return "redirect:/";
                case "topics":
                    //   return "redirect:/topics?categoryId=" + categoryId;
                case "pivottable":
                    return "redirect:/entry-filter/return?topicId=" + topic.getId();
                case "entries":
                    return "redirect:/entries?topicId=" + topic.getId();
                case "reporttable":
                    return "redirect:/reports/all";
                case "calorietracker":
                    return "redirect:/food?dateMillisYmd=" + dateMillisYmd;
                case "dayviewtracker":
                    return "redirect:/dayview?dateMillisYmd=" + dateMillisYmd;
                    // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
                    //   return "redirect:/" + returnPage + "?categoryId=" + categoryId;
            }
        }



        return "redirect:/topics?categoryId=" + categoryId;
    }

    // Mevcut topic'i düzenleme formu
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(required = false) String listReturn,
                               @RequestParam(name = "returnPage", required = false) String returnPage,
                               @RequestParam(name = "dateMillisYmd", required = false) Long dateMillisYmd,
                               Model model) {
        Topic topic = topicService
                .getTopicById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found!"));



        // Tüm kategorileri çekip modele ekle
        var categoryDTOList = categoryService.getAllCategories()
                .stream()
                .collect(Collectors.toList());
        model.addAttribute("categories", categoryDTOList);

        model.addAttribute("topic", topic);
        model.addAttribute("tagLines", String.join("\n", topicTags.tagsForTopic(id)));

        // basla
        List<Entry> entries = topic.getActivities();
        if(!entries.isEmpty()) {
            Long firstDoneEntryDateMillisYmd = entries.stream()
                    .filter(e -> e.getStatus() == 1) // 1 = done
                    .map(Entry::getDateMillisYmd)
                    .min(Long::compareTo)
                    .orElse(null);

            if (firstDoneEntryDateMillisYmd != null) {
                LocalDate firstDoneEntryDate = Instant
                        .ofEpochMilli(firstDoneEntryDateMillisYmd)
                        .atZone(AppTimeZoneProvider.getZone())
                        .toLocalDate();
                model.addAttribute("firstDoneEntryDateMillisYmd",firstDoneEntryDateMillisYmd);
                model.addAttribute("firstDoneEntryDate",firstDoneEntryDate);
            }
        }
        // bitti

        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        model.addAttribute("zoneId", zoneId);
        long todayMillisYmd = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli();
        model.addAttribute("todayMillisYmd", todayMillisYmd);

        List<String> imageFilePaths = collectImagePaths();
        model.addAttribute("imageFilePaths", imageFilePaths);

        model.addAttribute("availableParsers", parserRegistryLoader.getParserClassNames());
        model.addAttribute("returnPage", returnPage);

        model.addAttribute("dateMillisYmd", dateMillisYmd);
        //System.out.println("edit ********* " + dateMillisYmd);
        String listPath = PageReturnPath.topicList(listReturn);
        if (listPath != null) {
            model.addAttribute("listReturn", listPath);
            model.addAttribute("topicListPath", topic.getId() == null ? listPath : PageReturnPath.topicAnchor(listPath, topic.getId()));
        }
        return "topics/topic-form"; // Aynı formu kullanacağız
    }

    // Mevcut topic'i güncelleme
    @PostMapping("/update/{id}")
    @Transactional
    public String updateTopic(@PathVariable Long id,
                              @RequestParam(required = false) String tagLines,
                              @RequestParam(required = false) String listReturn,
                              @RequestParam(name = "returnPage", required = false) String returnPage,
                              @ModelAttribute("topic") Topic topic,
                              Model model,
                              @RequestParam(required = false) Long dateMillisYmd) {
        String listPath = PageReturnPath.topicList(listReturn);
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


        existingTopic.setImageFileName(topic.getImageFileName());
        existingTopic.setDataClassName(topic.getDataClassName());

        existingTopic.setIntervalRule(topic.getIntervalRule());

        // If the topic is changed while updating an existing record, the variables of the old and new topic are recalculated.
        topicService.updateTopicStatus(existingTopic.getId());
        if (tagLines != null) {
            topicTags.replaceTags(id, topicTags.parseTags(tagLines));
        }
        if (listPath != null) {
            return "redirect:" + PageReturnPath.topicAnchor(listPath, existingTopic.getId());
        }


        model.addAttribute("returnPage", returnPage);
        model.addAttribute("dateMillisYmd", dateMillisYmd);

        // Hangi sayfadan gelindiğini kontrol ediyoruz.
        if (returnPage != null) {
            switch (returnPage) {
                case "home":
                    return "redirect:/";
                case "topics":
                    //   return "redirect:/topics?categoryId=" + categoryId;
                case "pivottable":
                    return "redirect:/entry-filter/return?topicId=" + topic.getId();
                case "entries":
                    return "redirect:/entries?topicId=" + topic.getId();
                case "reporttable":
                    return "redirect:/reports/all";
                case "index":
                    // anasayfadaki radar grafigin altindaki topic isimlerine tiklayarak gelmissek oraya donuyoruz.
                    return "redirect:/";
                case "calorietracker":
                    return "redirect:/food?dateMillisYmd=" + dateMillisYmd;
                case "dayviewtracker":
                    return "redirect:/dayview?dateMillisYmd=" + dateMillisYmd;
                // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
                    //   return "redirect:/" + returnPage + "?categoryId=" + categoryId;
            }
        }

        return "redirect:/topics?categoryId=" + categoryId;
    }

    // Topic silme
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String deleteTopic(@PathVariable Long id, @RequestParam(required = false) String listReturn,
                              RedirectAttributes redirectAttributes) {
        String listPath = PageReturnPath.topicList(listReturn);

        Topic topic = topicService.getTopicById(id).get(); // for redirection after delete
        var categoryId = topic.getCategory().getId(); // for redirection after delete

        try {
            topicService.deleteTopic(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (listPath != null) {
            return "redirect:" + listPath;
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
