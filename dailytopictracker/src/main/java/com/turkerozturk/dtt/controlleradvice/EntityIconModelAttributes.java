package com.turkerozturk.dtt.controlleradvice;

import com.turkerozturk.dtt.controller.CategoryGroupController;
import com.turkerozturk.dtt.controller.EntryFilterController;
import com.turkerozturk.dtt.controller.SettingController;
import com.turkerozturk.dtt.controller.web.CategoryWebController;
import com.turkerozturk.dtt.controller.web.TopicReportCatGrpController;
import com.turkerozturk.dtt.controller.web.TopicReportController;
import com.turkerozturk.dtt.controller.web.TopicWebController;
import com.turkerozturk.dtt.helper.EntityIcons;
import com.turkerozturk.dtt.helper.SettingHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {CategoryGroupController.class, CategoryWebController.class, TopicWebController.class,
        EntryFilterController.class, TopicReportController.class, TopicReportCatGrpController.class, SettingController.class})
@RequiredArgsConstructor
public class EntityIconModelAttributes {
    private final SettingHelper settings;

    @ModelAttribute
    public void entityIconAttributes(Model model) {
        model.addAttribute("entityIconsEnabled", settings.getBoolean(EntityIcons.ENABLED_KEY, true));
        model.addAttribute("entityIconCatalog", EntityIcons.catalog());
    }
}
