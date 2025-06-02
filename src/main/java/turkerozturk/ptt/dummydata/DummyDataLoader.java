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
package turkerozturk.ptt.dummydata;


import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import turkerozturk.ptt.entity.*;
import turkerozturk.ptt.repository.*;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Eger veritabani YOKSA ve ayar dosyasinda rastgele veri izni verilmisse, veritabani olustuktan sonra icini rastgele
 * doldurur.
 */
@Component
@RequiredArgsConstructor
public class DummyDataLoader implements CommandLineRunner {

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;
    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;
    private final NoteRepository noteRepository;

    @Value("${dummy.database.create:0}")
    private String createDummyDatabase;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${dummy.category.count:5}")
    private String randomCategoryCount;

    @Value("${dummy.topic.count:50}")
    private String randomTopicCount;

    @Value("${dummy.entry.count:500}")
    private String randomEntryCount;

    @Value("${dummy.start.date.days.count:90}")
    private String daysCountBeforeToday;

    @Value("${dummy.end.date.days.count:90}")
    private String daysCountAfterToday;

    @Override
    public void run(String... args) throws Exception {

        //System.out.println("Checking dummy data loader conditions...");

        // Condition 1: Check if the property create.dummy.database is "1"
        if (Integer.parseInt(createDummyDatabase) != 1) {
            //System.out.println("DummyDataLoader disabled: In application.properties file" +
            //        ", 'dummy.database.create' property is not set to 1 (current value: " + createDummyDatabase + ").");
            return;
        }

        // Condition 2: Check if the SQLite file does NOT exist.
        // Assumes the datasourceUrl format is "jdbc:sqlite:mydatabase.db"
        /* this solution is not working because spring boot creates an empty database first, then this runner runs.
        String prefix = "jdbc:sqlite:";
        String filePath = datasourceUrl.startsWith(prefix) ? datasourceUrl.substring(prefix.length()) : "";
        File databaseFile = new File(filePath);
        if (databaseFile.exists()) {
            //System.out.println("DummyDataLoader disabled: The database file '" + filePath + "' already exists.");
            return;
        }
        */

        // Instead of checking for the existence of the SQLite file,
        // check if the Category table exists and is empty using the repository:
        // This is the solution:
        if (categoryRepository.count() > 0) {
            System.out.println("DummyDataLoader disabled: 'Category' table already exists and contains data.");
            return;
        }


        // Örnek: DataFaker importları
// import net.datafaker.Faker;
// import net.datafaker.providers.base.*;
// vb. gerekli importlarınız

// --- 1) Kategorileri ayarla ---
        Faker faker = new Faker();


        // *** CATEGORY_GROUPS ***
        int categoryGroupsCount = 4; // TODO properties file
        List<String> availableCategoryGroupNames = new ArrayList<>(
                Arrays.asList("Home Group", "Work Group", "Hobby Group", "Priority Group")
        );
        Collections.shuffle(availableCategoryGroupNames);

        // Kategori gruplarinin lerin isim tekrarini engellemek için
        Set<String> usedCategoryGroupNames = new HashSet<>();
        List<CategoryGroup> categoryGroups = new ArrayList<>();

        for (int i = 0; i < categoryGroupsCount; i++) {
            // Eğer listede yeterli kategori kalmadıysa, ek kategori isimlerini
            // datafaker’dan üretmek istiyorsanız (örneğin commerce().department),
            // aşağıdaki gibi bir fallback kullanabilirsiniz; burada opsiyonel bırakıyorum:
            String candidateName = i < availableCategoryGroupNames.size()
                    ? availableCategoryGroupNames.get(i)
                    : faker.commerce().department();

            while (usedCategoryGroupNames.contains(candidateName)) {
                candidateName = candidateName + "_" + faker.bothify("??##");
            }

            usedCategoryGroupNames.add(candidateName);
            CategoryGroup c = new CategoryGroup(candidateName);
            categoryGroups.add(categoryGroupRepository.save(c));
        }

        // *** CATEGORIES ***
        int categoryCount = Integer.parseInt(randomCategoryCount);

        // Örnek olarak kullanmak istediğimiz kategoriler:
        List<String> availableCategoryNames = new ArrayList<>(
                Arrays.asList("App", "Book", "Hobby", "Medication", "Movie", "Food", "Olympic Sport", "Music")
        );
        Collections.shuffle(availableCategoryNames);

        // Kategorilerin isim tekrarını engellemek için
        Set<String> usedCategoryNames = new HashSet<>();
        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < categoryCount; i++) {
            // Eğer listede yeterli kategori kalmadıysa, ek kategori isimlerini
            // datafaker’dan üretmek istiyorsanız (örneğin commerce().department),
            // aşağıdaki gibi bir fallback kullanabilirsiniz; burada opsiyonel bırakıyorum:
            String candidateName = i < availableCategoryNames.size()
                    ? availableCategoryNames.get(i)
                    : faker.commerce().department();

            while (usedCategoryNames.contains(candidateName)) {
                candidateName = candidateName + "_" + faker.bothify("??##");
            }

            usedCategoryNames.add(candidateName);

            // %20 ihtimalle is_archived = true
            boolean isArchived = faker.random().nextInt(1, 100) <= 20;

            // Rastgele bir categoryGroup seç (categoryGroups listesi dolu varsayılıyor)
            CategoryGroup randomGroup = categoryGroups.get(faker.random().nextInt(0, categoryGroups.size() - 1));

            // Yeni Category nesnesini oluştur ve gerekli alanları ata
            Category c = new Category(candidateName);
            c.setArchived(isArchived);
            c.setCategoryGroup(randomGroup);


            categories.add(categoryRepository.save(c));
        }

        // *** TOPIC ***
        List<Topic> topics = new ArrayList<>();

        for (int i = 0; i < Integer.parseInt(randomTopicCount); i++) {
            // Rastgele bir kategori seç
            Category randomCategory = categories.get(faker.random().nextInt(categories.size()));

            Topic t = new Topic();
            t.setCategory(randomCategory);

            // Kategori adına göre, topic adını ilgili DataFaker metoduyla çekiyoruz:
            switch (randomCategory.getName()) {
                case "App":
                    t.setName(faker.app().name());               // Örn: "CalendarPro"
                    break;
                case "Book":
                    t.setName(faker.book().title());             // Örn: "Lord of The Fakes"
                    break;
                case "Hobby":
                    t.setName(faker.hobby().activity());         // Örn: "Woodworking"
                    break;
                case "Medication":
                    t.setName(faker.medication().drugName());    // Örn: "Aspirin"
                    break;
                case "Movie":
                    t.setName(faker.movie().name());             // Örn: "Random Movie Title"
                    break;
                case "Food":
                    // örnek: "Pizza", "Sushi", vb.
                    t.setName(faker.food().dish());
                    break;
                case "Olympic Sport":
                    t.setName(faker.olympicSport().summerOlympics());
                    break;
                case "Music":
                    // örnek: "Piano", "Guitar", vb.
                    t.setName(faker.music().instrument());
                    break;
                default:
                    // Eğer fallback ya da fazladan bir kategoriye düşersek
                    t.setName(faker.lorem().word());
                    break;
            }

            // Kısa bir açıklama
            t.setDescription(faker.lorem().sentence());

            // %80 oranında 0, %15 oranında 1-7, %5 oranında 8-31
            int chance = faker.random().nextInt(1, 100);
            long someTimeLater;
            if (chance <= 80) {
                someTimeLater = 0L;
            } else if (chance <= 95) { // 80 < x <= 95
                someTimeLater = faker.number().numberBetween(1L, 8L); // 1-7 dahil
            } else { // 95 < x <= 100
                someTimeLater = faker.number().numberBetween(8L, 32L); // 8-31 dahil
            }
            t.setSomeTimeLater(someTimeLater);

            // %4 oranında true
            boolean pinned = faker.random().nextInt(1, 100) <= 4;
            t.setPinned(pinned);

            // %50 oranında -1, %30 oranında 0, %20 oranında 1-20 arası
            int weightChance = faker.random().nextInt(1, 100);
            int weight;
            if (weightChance <= 50) {
                weight = -1;
            } else if (weightChance <= 80) { // 50 < x <= 80
                weight = 0;
            } else { // 80 < x <= 100
                weight = faker.number().numberBetween(1, 21); // 1-20 dahil
            }
            t.setWeight(weight);



            topics.add(topicRepository.save(t));
        }

        // --- 3) Entry'leri, tarih aralığı + status + note içerikleriyle ekle ---
        //LocalDate startDate = LocalDate.parse(randomStartDate);
        //LocalDate endDate   = LocalDate.parse(randomEndDate);
        //long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(Integer.parseInt(daysCountBeforeToday));
        LocalDate endDate = today.plusDays(Integer.parseInt(daysCountAfterToday));
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Uniq. constraint set: "topicId:dateMillis"
        Set<String> usedTopicDatePairs = new HashSet<>();

        for (int i = 0; i < Integer.parseInt(randomEntryCount); i++) {
            Topic randomTopic = topics.get(faker.random().nextInt(topics.size()));

            long randomOffset = faker.number().numberBetween(0L, totalDays);
            LocalDate randomDate = startDate.plusDays(randomOffset);

            long dateMillis = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            String pairKey = randomTopic.getId() + ":" + dateMillis;

            int attempt = 0;
            while (usedTopicDatePairs.contains(pairKey) && attempt < 10) {
                randomOffset = faker.number().numberBetween(0L, totalDays);
                randomDate = startDate.plusDays(randomOffset);
                dateMillis = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                pairKey = randomTopic.getId() + ":" + dateMillis;
                attempt++;
            }

            if (usedTopicDatePairs.contains(pairKey)) {
                continue;
            }
            usedTopicDatePairs.add(pairKey);

            // Entry oluştur
            Entry entry = new Entry();
            entry.setTopic(randomTopic);
            entry.setDateMillisYmd(dateMillis);

            // Bugünden önce mi sonra mı kontrolü
            LocalDate entryDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate();
            int statusChance = faker.random().nextInt(1, 100);

            if (!entryDate.isAfter(today)) {
                // date <= today
                if (statusChance <= 90) {
                    entry.setStatus(1); // done
                } else if (statusChance > 90 && statusChance <= 97) {
                    entry.setStatus(0); // not marked
                } else {
                    entry.setStatus(2); // warning
                }
            } else {
                // date > today
                if (statusChance <= 99) {
                    entry.setStatus(0); // not marked
                } else {
                    entry.setStatus(2); // warning
                }
            }

            // *** NOTES ***
            Note note = new Note();

            int contentChance = faker.random().nextInt(1, 100);
            if (contentChance <= 70) {
                note.setContent(""); // %70 boş
            } else if (contentChance <= 90) {
                note.setContent(faker.lorem().characters(5, 20)); // %20 kısa metin
            } else {
                note.setContent(String.valueOf(faker.number().numberBetween(1, 51))); // %10 sayısal
            }

            note.setEntry(entry);
            entry.setNote(note);

            entryRepository.save(entry);
        }


        // *** FILL TOPIC dates: {prediction, done, warn, not marked} ***

        long todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (Topic topic : topics) {

            Long someTimeLater = topic.getSomeTimeLater();
            if (someTimeLater == null) {
                someTimeLater = 0L;
            }

            // 1. predictionDateMillisYmd = today + someTimeLater gün
            LocalDate predictionDate = LocalDate.now().plusDays(someTimeLater);
            long predictionDateMillis = predictionDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            topic.setPredictionDateMillisYmd(predictionDateMillis);

            // 2. Entry listesini çek
            List<Entry> topicEntries = entryRepository.findByTopicId(topic.getId());

            // 3. lastPastEntryDateMillisYmd: status = 1 && date <= today → max date
            topicEntries.stream()
                    .filter(e -> e.getStatus() == 1 && e.getDateMillisYmd() <= todayMillis)
                    .map(Entry::getDateMillisYmd)
                    .max(Long::compareTo)
                    .ifPresent(topic::setLastPastEntryDateMillisYmd);

            // 4. firstWarningEntryDateMillisYmd: status = 2 → min date
            topicEntries.stream()
                    .filter(e -> e.getStatus() == 2)
                    .map(Entry::getDateMillisYmd)
                    .min(Long::compareTo)
                    .ifPresent(topic::setFirstWarningEntryDateMillisYmd);

            // 5. firstFutureNeutralEntryDateMillisYmd: status = 0 && date >= today → min date
            topicEntries.stream()
                    .filter(e -> e.getStatus() == 0 && e.getDateMillisYmd() >= todayMillis)
                    .map(Entry::getDateMillisYmd)
                    .min(Long::compareTo)
                    .ifPresent(topic::setFirstFutureNeutralEntryDateMillisYmd);

            // Kaydet
            topicRepository.save(topic);
        }


        System.out.println("Dummy data created.");
    }
}

