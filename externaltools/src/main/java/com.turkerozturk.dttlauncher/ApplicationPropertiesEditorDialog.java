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
package com.turkerozturk.dttlauncher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ApplicationPropertiesEditorDialog extends JDialog {

    private static final String FILE_NAME = "application.properties";

    private final File externalFile;
    private final Properties defaultProps = new Properties();
    private final Properties localProps = new Properties();

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField filterField;
    private JButton saveButton;

    public ApplicationPropertiesEditorDialog(Frame owner) {
        super(owner, "Application Properties Editor", true);
        setSize(800, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        externalFile = new File(System.getProperty("user.dir"), FILE_NAME);
        loadProperties();
        setupUI();
    }

    /** Varsayılan (jar içi) ve yerel (dosya sistemi) properties dosyalarını yükler. */
    private void loadProperties() {
        // 1️⃣ daily-topic-tracker.jar içindeki varsayılan dosyayı oku

        loadDefaultPropertiesFromExternalJar();


        // 2️⃣ Klasördeki mevcut application.properties'i oku (yoksa boş kalır)
        if (externalFile.exists()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                localProps.load(fis);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Cannot read local properties: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }






    private void loadDefaultPropertiesFromExternalJar() {
        File jarFile = new File(System.getProperty("user.dir"), "daily-topic-tracker.jar");
        if (!jarFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "daily-topic-tracker.jar not found in the same directory.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry entry = jar.getEntry("application.properties");
            if (entry == null) {
                // Spring Boot jar yapısında BOOT-INF altına bak
                entry = jar.getEntry("BOOT-INF/classes/application.properties");
            }

            if (entry == null) {
                JOptionPane.showMessageDialog(this,
                        "application.properties not found inside daily-topic-tracker.jar.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (InputStream in = jar.getInputStream(entry)) {
                loadUtf8PropertiesIgnoringComments(in, defaultProps);
            }


        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error reading daily-topic-tracker.jar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUtf8PropertiesIgnoringComments(InputStream in, Properties target) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // UTF-8 BOM temizle
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                // Boş satır veya yorum satırı atla
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    target.setProperty(key, val);
                }
            }
        }
    }


    /** Tabloyu ve filtreyi oluşturur. */
    private void setupUI() {
        // --- Tablo modelini oluştur ---
        tableModel = new DefaultTableModel(new Object[]{"Key", "Default Value", "Your Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // yalnızca "Your Value" düzenlenebilir
            }
        };

        // Key’leri alfabetik sıraya koy



        // defaultProps: Properties
        TreeMap<String, String> sortedKeys = new TreeMap<>();
        for (String key : defaultProps.stringPropertyNames()) {
            sortedKeys.put(key, defaultProps.getProperty(key));
        }


        // --- Tablonun satırlarını doldur ---
        for (Map.Entry<String, String> entry : sortedKeys.entrySet()) {
            String key = entry.getKey();
            String defValue = entry.getValue();
            String userValue = localProps.getProperty(key, "");
            tableModel.addRow(new Object[]{key, defValue, userValue});
        }

        // --- Tabloyu oluştur ---
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowSorter(new TableRowSorter<>(tableModel));

        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        table.getInputMap(JComponent.WHEN_FOCUSED).put(copy, "copyCell");

        table.getActionMap().put("copyCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();
                if (row >= 0 && col >= 0) {
                    Object value = table.getValueAt(row, col);
                    StringSelection selection = new StringSelection(value == null ? "" : value.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String key = (String) table.getValueAt(row, 0);
                boolean existsInLocal = localProps.containsKey(key);

                if (!existsInLocal && !isSelected) {
                    c.setBackground(new Color(220, 255, 220)); // açık yeşil arka plan
                } else if (!isSelected) {
                    c.setBackground(Color.white);
                }
                return c;
            }
        });



        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // --- Arama paneli ---
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        filterField = new JTextField();
        filterPanel.add(filterField, BorderLayout.CENTER);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = filterField.getText();
                TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
                if (text.trim().isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0));
            }

            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        // --- Alt kısımda Save butonu ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save and Restart Required");
        saveButton.addActionListener(e -> saveProperties());
        bottomPanel.add(saveButton);

        // --- Bileşenleri ekle ---
        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** Yeni değerleri external application.properties dosyasına kaydeder. */
    private void saveProperties() {
        // Güncel değerleri modele göre localProps’a aktar
        localProps.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String key = (String) tableModel.getValueAt(i, 0);
            String value = (String) tableModel.getValueAt(i, 2);
            if (value != null && !value.trim().isEmpty()) {
                localProps.setProperty(key, value.trim());
            }
        }

        // Dosyayı kaydet
        try (FileOutputStream out = new FileOutputStream(externalFile)) {
            localProps.store(out, "Updated by LaunchDTT Properties Editor");
            JOptionPane.showMessageDialog(this,
                    "Settings saved successfully.\nPlease restart the application for changes to take effect.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Test için bağımsız çalıştırma
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ApplicationPropertiesEditorDialog dialog = new ApplicationPropertiesEditorDialog(null);
            dialog.setVisible(true);
        });
    }
}
