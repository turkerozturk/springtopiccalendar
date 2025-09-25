package com.turkerozturk.dtt.controller.web;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.service.EntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
public class ChartRestController {

    @Autowired
    private EntryService entryService;

    @Autowired
    private CategoryRepository categoryRepository;

    ZoneId zoneId = AppTimeZoneProvider.getZone();


    @GetMapping("/radar")
    public Map<String, Object> getRadarChartData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }

        long dateMillisYmd = date.atStartOfDay(zoneId).toInstant().toEpochMilli();

        List<Category> categories = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        int totalCount = 0;

        int totalWeight = 0;

        for (Category c : categories) {
            List<Entry> doneEntries = entryService.findDonesByCategory(c.getId(), dateMillisYmd);

            List<Entry> weightedEntries = new ArrayList<>();
            for(Entry e : doneEntries) {
                if(e.getTopic().getWeight() >= 0) {
                    weightedEntries.add(e);
                }
            }


            if (!weightedEntries.isEmpty()) {
                labels.add(c.getName());
                counts.add(weightedEntries.size());
                ids.add(c.getId());
                totalCount += weightedEntries.size();
                for(Entry entry : weightedEntries) {

                    Topic topic = entry.getTopic();
                    if(topic.getWeight() > 0) {
                        totalWeight += topic.getWeight();

                        //  System.out.println(topic.getWeight() + "\t" + modelPrefix + "\t" + topic.getName());
                    }
                }


            }

        }





        // burada senin prepareCategoryPieChartForDate() içindeki hesapları yapıyoruz
        Map<String, Object> result = new HashMap<>();
        result.put("totalWeight", totalWeight); // örnek
        result.put("categoryLabels", labels);
        result.put("categoryCounts", counts);
        //result.put("categoryIds", ids);
        result.put("categoryTotalCount", totalCount);
        result.put("categoryTotalCategories", labels.size());

        return result;
    }
}
