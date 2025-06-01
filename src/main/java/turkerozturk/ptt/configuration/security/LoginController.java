package turkerozturk.ptt.configuration.security;


import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 https://www.youtube.com/watch?v=aMd3P_5bB6s
 Spring Security 6: Personalize Your Login Experience
 */
@Controller
public class LoginController {

    /**
     * https://www.youtube.com/watch?v=aMd3P_5bB6s
     *  Spring Security 6: Personalize Your Login Experience
     * @return
     */
    @GetMapping("/login")
    public String login() {
        return "login/login";
    }






}
