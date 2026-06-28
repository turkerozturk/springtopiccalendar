package com.turkerozturk.dtt.dto.notefieldstructures;

import com.turkerozturk.dtt.entity.Entry;

import java.util.List;

public interface NoteFieldStructure {

    public String getName();
    public String getDescription();

    public void parseRawData(List<Entry> entries);

    public String getParsedDataAsJSON();

    public String getReport();

    // amaci bir tarih araliginin ortalamasi alinirken kac gun uzerinden alinmasi gerektigi.
    // TODO bu da iyi bir cozum olmadi. Bence tarih araliginin kendisini gondermeliyiz.
    //  Boylece entry icermeyen bos gunler icin sifir degeri yazabiliriz veya formul kullanarak tahmini
    //  degerler uretebiliriz.
    public void setCustomDaysCounter(int customDaysCounter);
}
