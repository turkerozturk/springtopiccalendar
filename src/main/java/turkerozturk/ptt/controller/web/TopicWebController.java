package turkerozturk.ptt.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.service.CategoryService;
import turkerozturk.ptt.service.TopicService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/topics")
public class TopicWebController {

    @Autowired
    private TopicService topicService;

    @Autowired
    private CategoryService categoryService;

    // Tüm topic'leri veya belli kategoriye ait topic'leri listeleyen sayfa
    @GetMapping
    public String listTopics(@RequestParam(value = "categoryId", required = false) Long categoryId,
                             Model model) {
        // Tüm kategorileri drop-down'da göstermek için çekelim
        var categoryList = categoryService.getAllCategories();

        // Eğer kategori ID geldiyse filtreli liste, yoksa tüm liste
        List<Topic> topicDTOList;
        if (categoryId != null) {
            topicDTOList = topicService.getTopicsByCategoryId(categoryId);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            topicDTOList = topicService.getAllTopics();
            // Seçili kategori yoksa null bırakabiliriz
            model.addAttribute("selectedCategoryId", null);
        }

        // Modele eklemeler
        model.addAttribute("topics", topicDTOList);
        model.addAttribute("categories", categoryList);

        return "topics"; // templates/topics.html
    }



    // Yeni topic oluşturma formu
    @GetMapping("/create")
    public String showCreateForm(
            @RequestParam(name="categoryId", required=false) Long categoryId,
            Model model) {

        // Yeni boş Topic nesnesi (formda doldurulacak)
        Topic topic = new Topic();

        // Eğer bir categoryId gelmişse, bu ID'ye karşılık gelen Category'yi bulup topic’e set ediyoruz
        if (categoryId != null) {
            Category cat = categoryService.getCategoryById(categoryId).orElse(null);

                topic.setCategory(cat);

        }



        // Boş bir DTO
        model.addAttribute("topicDTO", topic);

        // Kategori seçimi için tüm kategorileri DTO olarak modele ekleyelim
        var categoryDTOList = categoryService.getAllCategories()
                .stream()
                .collect(Collectors.toList());
        model.addAttribute("categories", categoryDTOList);

        return "topic-form"; // templates/topic-form.html
    }

    // Yeni topic kaydetme
    @PostMapping
    public String saveTopic(@ModelAttribute("topicDTO") Topic topic) {
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

        return "redirect:/topics";
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

        model.addAttribute("topicDTO", topic);
        return "topic-form"; // Aynı formu kullanacağız
    }

    // Mevcut topic'i güncelleme
    @PostMapping("/update/{id}")
    public String updateTopic(@PathVariable Long id,
                              @ModelAttribute("topicDTO") Topic topicDTO) {
        // Veritabanından topic bul
        Topic existingTopic = topicService
                .getTopicById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found!"));

        // Topic bilgilerini güncelle
        existingTopic.setName(topicDTO.getName());
        existingTopic.setDescription(topicDTO.getDescription());

        // Kategori güncelle
        var categoryId = topicDTO.getCategory().getId();
        Category category = categoryService
                .getCategoryById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found!"));
        existingTopic.setCategory(category);

        // Kayıt
        topicService.saveTopic(existingTopic);

        return "redirect:/topics";
    }

    // Topic silme
    @GetMapping("/delete/{id}")
    public String deleteTopic(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            topicService.deleteTopic(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/topics";
    }


}
