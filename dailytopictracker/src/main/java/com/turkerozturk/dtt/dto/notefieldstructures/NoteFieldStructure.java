package com.turkerozturk.dtt.dto.notefieldstructures;

import com.turkerozturk.dtt.entity.Entry;

import java.util.List;

public interface NoteFieldStructure {

    public String getName();
    public String getDescription();

    public void parseRawData(List<Entry> entries);

    public String getParsedDataAsJSON();

    public String getReport();
}
