package com.turkerozturk.dtt.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CategoryProfile.java
@Data
@Entity
@Table(name = "category_profiles")
public class CategoryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "category_profile_categories",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // yukaridaki set'i sort edemedigimiz icin yapay zeka asagidaki cozumu onerdi:
    @Transient
    private List<Category> categoriesSorted;





}
