package turkerozturk.ptt.configuration.multilanguage;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LanguageController {

    @GetMapping("/languages")
    public String showLanguagePage() {
        return "language/languages"; // templates/language.html dosyasını gösterir
    }
}
