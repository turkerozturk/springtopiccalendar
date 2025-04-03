package turkerozturk.ptt.dummydata;


import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
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


        // --- 2) Faker ayarla ve 50 Topic ekle ---
        Faker faker = new Faker();

        // --- 1) Önce kategorileri ekleyelim ---
        // --- Category Generation using Faker and randomCategoryCount ---
        int categoryCount = Integer.parseInt(randomCategoryCount);
        List<String> availableCategoryNames = new ArrayList<>(Arrays.asList("Food", "Health", "Sport", "Work", "Payments", "Projects"));
        Collections.shuffle(availableCategoryNames);
        Set<String> usedCategoryNames = new HashSet<>();
        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < categoryCount; i++) {
            // Use a predefined name if available, otherwise generate one via Faker.
            String candidateName = i < availableCategoryNames.size()
                    ? availableCategoryNames.get(i)
                    : faker.commerce().department();

            // Ensure the candidateName is unique; if not, append a random alphanumeric suffix.
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
            Category randomCategory = categories.get(faker.random().nextInt(categories.size()));
            Topic t = new Topic();
            t.setCategory(randomCategory);
            t.setName(faker.book().title());         // Örnek olarak "Random Book Title"
            t.setDescription(faker.lorem().sentence()); // Kısa açıklama
            topics.add(topicRepository.save(t));
        }

        // --- 3) iki tarih arası tarih aralığında Entryleri ekle ---
        // --- Parsing date properties into LocalDate ---
        LocalDate startDate = LocalDate.parse(randomStartDate);  // e.g., "2025-03-01"
        LocalDate endDate   = LocalDate.parse(randomEndDate);    // e.g., "2025-05-01"
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // (startDate ile endDate dahil fark + 1 gün)

        // Uniq. constraint için kullanılan set: "topicId:dateMillis" şeklinde
        Set<String> usedTopicDatePairs = new HashSet<>();

        for (int i = 0; i < Integer.parseInt(randomEntryCount); i++) {
            // Rastgele bir topic seç
            Topic randomTopic = topics.get(faker.random().nextInt(topics.size()));

            // Rastgele bir gün seç (0 ile totalDays-1 arası)
            long randomOffset = faker.number().numberBetween(0L, totalDays);
            LocalDate randomDate = startDate.plusDays(randomOffset);

            // Sadece tarih bazlı epoch (00:00:00)
            long dateMillis = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // Tekrarlanma ihtimaline karşı 10 denemeye kadar yeni tarih arayabiliriz
            String pairKey = randomTopic.getId() + ":" + dateMillis;
            int attempt = 0;
            while (usedTopicDatePairs.contains(pairKey) && attempt < 10) {
                randomOffset = faker.number().numberBetween(0L, totalDays);
                randomDate   = startDate.plusDays(randomOffset);
                dateMillis   = randomDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                pairKey      = randomTopic.getId() + ":" + dateMillis;
                attempt++;
            }

            // Son denemede hala varsa, bu kaydı atla
            if (usedTopicDatePairs.contains(pairKey)) {
                continue;
            }
            usedTopicDatePairs.add(pairKey);

            // Entry oluştur
            Entry entry = new Entry();
            entry.setTopic(randomTopic);
            entry.setDateMillisYmd(dateMillis);
            // status = [0,1,2] => 0=not marked, 1=done, 2=warning
            entry.setStatus(faker.number().numberBetween(0, 3));

            // Note oluştur
            Note note = new Note();
            note.setContent(faker.lorem().paragraph()); // Lorem ipsum benzeri içerik
            // Note ile Entry arasındaki ilişkiyi kur
            note.setEntry(entry);
            entry.setNote(note);

            // Kaydet
            entryRepository.save(entry);
        }

        System.out.println("Dummy data yüklemesi tamamlandı!");
    }
}

