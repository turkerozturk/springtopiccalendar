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
package turkerozturk.ptt.configuration.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String login(Model model, @Value("${spring.security.user.password}") String password) {
        boolean isDefaultPassword = "{noop}admin".equals(password);
        model.addAttribute("isDefaultPassword", isDefaultPassword);
        return "login/login";
    }







}
