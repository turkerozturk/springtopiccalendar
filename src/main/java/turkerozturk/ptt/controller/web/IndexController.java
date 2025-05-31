package turkerozturk.ptt.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import turkerozturk.ptt.repository.CategoryGroupRepository;
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;

@Controller
public class IndexController {

    @Autowired
    CategoryGroupRepository categoryGroupRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    EntryRepository entryRepository;

    @GetMapping
    public String homePage(Model model) {


        model.addAttribute("categoryGroupsCount", categoryGroupRepository.count());
        model.addAttribute("categoriesCount", categoryRepository.count());
        model.addAttribute("topicsCount", topicRepository.count());
        model.addAttribute("entriesCount", entryRepository.count());

        return "index";
    }


}
