package com.turkerozturk.dtt.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryProfileForm {
    private Long id;
    private String name;
    private List<Long> selectedCategoryIds = new ArrayList<>();
}
