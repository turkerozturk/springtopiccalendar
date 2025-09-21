package com.turkerozturk.dtt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntryDto {

    private Long id;
    private Integer status;
    private boolean hasNote;

}
