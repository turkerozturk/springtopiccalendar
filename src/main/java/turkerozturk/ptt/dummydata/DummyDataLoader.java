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
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.NoteRepository;

import java.io.File;
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

    @Value("${dummy.start.date:2025-03-01}")
    private String randomStartDate;

    @Value("${dummy.end.date:2025-05-01}")
    private String randomEndDate;

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
            Category c = new Category(candidateName);
            categories.add(categoryRepository.save(c));
        }

// --- 2) Topic ekle ---
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
            topics.add(topicRepository.save(t));
        }

// --- 3) Entry'leri, tarih aralığı + status + note içerikleriyle ekle ---
        LocalDate startDate = LocalDate.parse(randomStartDate);
        LocalDate endDate   = LocalDate.parse(randomEndDate);
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
            entry.setStatus(faker.number().numberBetween(0, 3)); // 0=not marked,1=done,2=warning

            // Note ilişkisi
            Note note = new Note();
            note.setContent(faker.lorem().paragraph());
            note.setEntry(entry);
            entry.setNote(note);

            entryRepository.save(entry);
        }


        System.out.println("Dummy data yüklemesi tamamlandı!");
    }
}

