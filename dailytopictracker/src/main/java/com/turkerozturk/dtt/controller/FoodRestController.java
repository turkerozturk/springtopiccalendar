package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.dto.TopicDto;
import com.turkerozturk.dtt.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class FoodRestController {

    @Autowired
    private TopicRepository topicRepository;

    @GetMapping("/api/food-topics")
    @ResponseBody
    public List<TopicDto> searchFoodTopics(@RequestParam String q) {
        return topicRepository.searchFoodTopics(q);
    }

}
