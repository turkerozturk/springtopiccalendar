package turkerozturk.ptt.controlleradvice;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import turkerozturk.ptt.repository.CategoryGroupRepository;
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    public GlobalModelAttributes(CategoryGroupRepository categoryGroupRepository,
                                 CategoryRepository categoryRepository,
                                 TopicRepository topicRepository,
                                 EntryRepository entryRepository) {
        this.categoryGroupRepository = categoryGroupRepository;
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("hasCategoryGroups", categoryGroupRepository.count() > 0);
        model.addAttribute("hasCategories", categoryRepository.count() > 0);
        model.addAttribute("hasTopics", topicRepository.count() > 0);
        model.addAttribute("hasEntries", entryRepository.count() > 0);
    }
}

