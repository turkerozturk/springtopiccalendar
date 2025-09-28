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

        long dateMillisYmd;
        if (date == null) {
            dateMillisYmd = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli();
        } else {
            dateMillisYmd = date.atStartOfDay(zoneId).toInstant().toEpochMilli();
        }

        List<Category> categories = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        List<String> labels = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Integer> categoryWeights = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        StringBuilder reportForPositiveWeight = new StringBuilder();
        StringBuilder reportForZeroWeight = new StringBuilder();
        StringBuilder reportForNegativeWeight = new StringBuilder();

        int totalCount = 0;

        int totalWeight = 0;

        for (Category c : categories) {
            List<Entry> doneEntries = entryService.findDonesByCategory(c.getId(), dateMillisYmd);

            List<Entry> weightedEntries = new ArrayList<>();
            for(Entry e : doneEntries) {
                if(e.getTopic().getWeight() >= 0) {
                    weightedEntries.add(e);
                } else {
                    Topic topic = e.getTopic();

                    reportForNegativeWeight.append("<div style='text-align: left;'>");
                    reportForNegativeWeight.append(topic.getWeight());

                    reportForNegativeWeight.append("✎");
                    reportForNegativeWeight.append("<a class='topic-name' href='/topics/edit/");
                    reportForNegativeWeight.append(topic.getId());
                    reportForNegativeWeight.append("?returnPage=index");
                    reportForNegativeWeight.append("'>");
                    reportForNegativeWeight.append(topic.getName());
                    reportForNegativeWeight.append("</a>");

                    reportForNegativeWeight.append("⚙");
                    reportForNegativeWeight.append("<a class='category-name' href='/entry-filter/form?categoryId=");
                    reportForNegativeWeight.append(topic.getCategory().getId());
                    reportForNegativeWeight.append("'>");
                    reportForNegativeWeight.append(topic.getCategory().getName());
                    reportForNegativeWeight.append("</a>");

                    reportForNegativeWeight.append("</div>");
                }
            }

            int categoryWeight = 0;

            if (!weightedEntries.isEmpty()) {
                labels.add(c.getName());
                counts.add(weightedEntries.size());
                ids.add(c.getId());
                totalCount += weightedEntries.size();
                for(Entry entry : weightedEntries) {

                    Topic topic = entry.getTopic();
                    if(topic.getWeight() > 0) {
                        totalWeight += topic.getWeight();
                        categoryWeight += topic.getWeight();

                          //System.out.println(topic.getWeight() + "\t" +  topic.getName());
                        reportForPositiveWeight.append("<div style='text-align: left;'>");
                        reportForPositiveWeight.append(topic.getWeight());
                        reportForPositiveWeight.append("✎");

                        reportForPositiveWeight.append("<a class='topic-name' href='/topics/edit/");
                        reportForPositiveWeight.append(topic.getId());
                        reportForPositiveWeight.append("?returnPage=index");
                        reportForPositiveWeight.append("'>");
                        reportForPositiveWeight.append(topic.getName());
                        reportForPositiveWeight.append("</a>");

                        reportForPositiveWeight.append("⚙");
                        reportForPositiveWeight.append("<a class='category-name' href='/entry-filter/form?categoryId=");
                        reportForPositiveWeight.append(topic.getCategory().getId());
                        reportForPositiveWeight.append("'>");
                        reportForPositiveWeight.append(topic.getCategory().getName());
                        reportForPositiveWeight.append("</a>");

                        reportForPositiveWeight.append("</div>");
                    } else if(topic.getWeight() == 0) {
                        reportForZeroWeight.append("<div style='text-align: left;'>");
                        reportForZeroWeight.append(topic.getWeight());
                        reportForZeroWeight.append("✎");

                        reportForZeroWeight.append("<a class='topic-name' href='/topics/edit/");
                        reportForZeroWeight.append(topic.getId());
                        reportForZeroWeight.append("?returnPage=index");
                        reportForZeroWeight.append("'>");
                        reportForZeroWeight.append(topic.getName());
                        reportForZeroWeight.append("</a>");

                        reportForZeroWeight.append("⚙");
                        reportForZeroWeight.append("<a class='category-name' href='/entry-filter/form?categoryId=");
                        reportForZeroWeight.append(topic.getCategory().getId());
                        reportForZeroWeight.append("'>");
                        reportForZeroWeight.append(topic.getCategory().getName());
                        reportForZeroWeight.append("</a>");

                        reportForZeroWeight.append("</div>");
                    } else {
                        // reportForNegativeWeight islemlerini burada degil, blok disinda yukarida hallettik zaten.
                    }
                }
                categoryWeights.add(categoryWeight);


            }

        }





        // burada senin prepareCategoryPieChartForDate() içindeki hesapları yapıyoruz
        Map<String, Object> result = new HashMap<>();
        result.put("totalWeight", totalWeight);
        result.put("categoryLabels", labels);
        result.put("categoryCounts", counts);
        result.put("categoryWeights", categoryWeights);
        //result.put("categoryIds", ids);
        result.put("categoryTotalCount", totalCount);
        result.put("categoryTotalCategories", labels.size());

        result.put("reportForPositiveWeight", reportForPositiveWeight.toString());
        result.put("reportForZeroWeight", reportForZeroWeight.toString());
        result.put("reportForNegativeWeight", reportForNegativeWeight.toString());

        return result;
    }
}
