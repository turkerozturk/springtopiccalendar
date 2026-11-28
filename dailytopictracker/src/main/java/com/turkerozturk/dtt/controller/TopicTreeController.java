package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.entity.CategoryProfile;
import com.turkerozturk.dtt.helper.EntityIcons;
import com.turkerozturk.dtt.helper.SettingHelper;
import com.turkerozturk.dtt.service.CategoryProfileService;
import com.turkerozturk.dtt.service.TopicService;
import com.turkerozturk.dtt.service.TopicTagService;
import com.turkerozturk.dtt.service.TopicTreeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/topic-tree")
public class TopicTreeController {
    public static final String SHOW_DATE = "ui.topicTree.showLastEntryDate";
    public static final String SHOW_TAGS = "ui.topicTree.showTags";

    private final TopicTreeService treeService;
    private final TopicService topicService;
    private final TopicTagService tags;
    private final CategoryProfileService profiles;
    private final SettingHelper settings;
    private final AppTimeZoneProvider timeZone;
    private final MarkdownService markdown;
    private final String databaseName;

    public TopicTreeController(TopicTreeService treeService, TopicService topicService, TopicTagService tags,
                               CategoryProfileService profiles, SettingHelper settings, AppTimeZoneProvider timeZone,
                               MarkdownService markdown, @Value("${spring.datasource.url:}") String dataSourceUrl) {
        this.treeService = treeService;
        this.topicService = topicService;
        this.tags = tags;
        this.profiles = profiles;
        this.settings = settings;
        this.timeZone = timeZone;
        this.markdown = markdown;
        String path = dataSourceUrl.replaceFirst("^jdbc:sqlite:(?:file:)?", "").split("\\?", 2)[0].replace('\\', '/');
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        this.databaseName = fileName.isBlank() || ":memory:".equals(fileName) ? "Database" : fileName;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("showTreeDate", settings.getBoolean(SHOW_DATE, true));
        model.addAttribute("showTreeTags", settings.getBoolean(SHOW_TAGS, true));
        model.addAttribute("showTreeIcons", settings.getBoolean(EntityIcons.ENABLED_KEY, true));
        model.addAttribute("topicTags", tags.allTags());
        return "topics/topic-tree";
    }

    @GetMapping("/nodes")
    @ResponseBody
    public TreeData nodes(@RequestParam MultiValueMap<String, String> params) {
        var profile = profiles.getActiveProfile().orElse(null);
        List<TopicTreeService.Node> groups = treeService.groups(profile == null ? null : profile.getId(),
                params.getOrDefault("includeTag", List.of()), params.getOrDefault("excludeTag", List.of()));
        return new TreeData(databaseName, profile == null ? null : profile.getName(), groups);
    }

    @GetMapping("/topic/{id}")
    public String topic(@PathVariable long id, @RequestParam(defaultValue = "false") boolean saved, Model model) {
        var topic = topicService.getTopicById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var category = topic.getCategory();
        boolean visible = !category.isArchived() && profiles.getActiveProfile()
                .map(CategoryProfile::getCategories)
                .map(categories -> categories.stream().anyMatch(item -> item.getId().equals(category.getId())))
                .orElse(true);
        if (!visible) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("topic", topic);
        model.addAttribute("descriptionHtml", markdown.render(topic.getDescription()));
        model.addAttribute("topicTags", tags.tagsForTopic(id));
        model.addAttribute("entrySummary", treeService.entrySummary(id));
        model.addAttribute("today", LocalDate.now(timeZone.getZoneId()));
        model.addAttribute("saved", saved);
        return "topics/topic-tree-panel :: panel";
    }

    public record TreeData(String databaseName, String profileName, List<TopicTreeService.Node> groups) {
    }
}
