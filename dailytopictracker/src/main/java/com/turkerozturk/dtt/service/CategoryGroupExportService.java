package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.entity.*;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryGroupExportService {

    private final DataSource mainDataSource;
  //  private final Flyway flyway;
    private final CategoryGroupRepository categoryGroupRepository;

    public Path exportCategoryGroups(List<Long> categoryGroupIds) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = "dtt-cg-" + timestamp + ".sqlite";
        Path exportPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);

        // 1️⃣ Yeni SQLite DB oluştur
        createEmptyDatabase(exportPath);

        // 2️⃣ Flyway migrationlarını bu dosyada çalıştır
        applyFlywayMigrations(exportPath);

        // 3️⃣ Seçilen CategoryGroup verilerini getir
        List<CategoryGroup> groups = categoryGroupRepository.findAllById(categoryGroupIds);

        // 4️⃣ JDBC bağlantısı aç ve verileri yaz
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + exportPath.toAbsolutePath())) {
            conn.setAutoCommit(false);
            try {
                insertData(conn, groups);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        return exportPath;
    }

    private void createEmptyDatabase(Path path) throws IOException {
        Files.deleteIfExists(path);
        Files.createFile(path);
    }

    private void applyFlywayMigrations(Path sqliteFile) {
        Flyway flywayForExport = Flyway.configure()
                .dataSource("jdbc:sqlite:" + sqliteFile.toAbsolutePath(), null, null)
                .locations("classpath:db/migration")
                .load();
        flywayForExport.migrate();
    }

    public Path createEmptyExportDatabase() throws IOException {
        // 1️⃣ Dosya ismini oluştur
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = "dtt-cg-" + timestamp + ".sqlite";
        Path exportPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);

        // 2️⃣ Aynı isimli dosya varsa sil
        Files.deleteIfExists(exportPath);

        // 3️⃣ Boş dosya oluştur
        Files.createFile(exportPath);

        // 4️⃣ Flyway migrationlarını çalıştır
        applyFlywayMigrations(exportPath);

        return exportPath;
    }

    // Test amacıyla tablo var mı kontrol etmek istersen:
    public boolean hasAnyTables(Path sqliteFile) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.toAbsolutePath())) {
            var rs = conn.getMetaData().getTables(null, null, "%", null);
            return rs.next();
        }
    }

    private void insertData(Connection conn, List<CategoryGroup> groups) throws SQLException {
        for (CategoryGroup cg : groups) {
            insertCategoryGroup(conn, cg);
            for (Category cat : cg.getCategories()) {
                insertCategory(conn, cat, cg.getId());
                for (Topic topic : cat.getTopics()) {
                    insertTopic(conn, topic, cat.getId());
                    for (Entry entry : topic.getActivities()) {
                        insertEntry(conn, entry, topic.getId());
                        if (entry.getNote() != null) {
                            insertNote(conn, entry.getNote(), entry.getId());
                        }
                    }
                }
            }
        }
    }

    private void insertCategoryGroup(Connection conn, CategoryGroup cg) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO category_groups (id, name, priority, background_color, image_file_name) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, cg.getId());
            ps.setString(2, cg.getName());
            ps.setInt(3, cg.getPriority());
            ps.setString(4, cg.getBackgroundColor());
            ps.setString(5, cg.getImageFileName());
            ps.executeUpdate();
        }
    }

    private void insertCategory(Connection conn, Category cat, Long categoryGroupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO categories (id, name, is_archived, category_group_number) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, cat.getId());
            ps.setString(2, cat.getName());
            ps.setInt(3, cat.isArchived() ? 1 : 0);
            ps.setLong(4, categoryGroupId);
            ps.executeUpdate();
        }
    }

    /**
     * yapay zeka: ps.setObject(..., java.sql.Types.BIGINT) → Long nullable oldugu icin, null degerlerde hata vermemesi adina setObject kullandik.
     * @param conn
     * @param topic
     * @param categoryId
     * @throws SQLException
     */
    private void insertTopic(Connection conn, Topic topic, Long categoryId) throws SQLException {
        String sql = """
        INSERT INTO topics (
            id, name, description, some_time_later, is_pinned, 
            data_class_name, image_file_name, interval_rule,
            prediction_date_millis_ymd, last_past_entry_date_millis_ymd, 
            first_warning_entry_date_millis_ymd, first_future_neutral_entry_date_millis_ymd,
            base_date_millis_ymd, end_date_millis_ymd, weight, category_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topic.getId());
            ps.setString(2, topic.getName());
            ps.setString(3, topic.getDescription());
            ps.setObject(4, topic.getSomeTimeLater(), java.sql.Types.BIGINT);
            ps.setInt(5, topic.isPinned() ? 1 : 0);
            ps.setString(6, topic.getDataClassName());
            ps.setString(7, topic.getImageFileName());
            ps.setString(8, topic.getIntervalRule());
            ps.setObject(9, topic.getPredictionDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setObject(10, topic.getLastPastEntryDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setObject(11, topic.getFirstWarningEntryDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setObject(12, topic.getFirstFutureNeutralEntryDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setObject(13, topic.getBaseDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setObject(14, topic.getEndDateMillisYmd(), java.sql.Types.BIGINT);
            ps.setInt(15, topic.getWeight());
            ps.setLong(16, categoryId);

            ps.executeUpdate();
        }
    }


    private void insertEntry(Connection conn, Entry entry, Long topicId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO entries (id, topic_id, date_millis_ymd, status) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, entry.getId());
            ps.setLong(2, topicId);
            ps.setLong(3, entry.getDateMillisYmd());
            ps.setInt(4, entry.getStatus());
            ps.executeUpdate();
        }
    }

    private void insertNote(Connection conn, Note note, Long entryId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notes (id, content, entry_id) VALUES (?, ?, ?)")) {
            ps.setLong(1, note.getId());
            ps.setString(2, note.getContent());
            ps.setLong(3, entryId);
            ps.executeUpdate();
        }
    }
}

