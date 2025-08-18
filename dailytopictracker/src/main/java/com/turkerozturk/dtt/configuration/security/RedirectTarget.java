package com.turkerozturk.dtt.configuration.security;

public enum RedirectTarget {
    HOME("/"),
    CATEGORY_GROUPS("/category-groups"),
    CATEGORIES("/categories"),
    TOPICS("/topics"),
    ENTRIES("/entries"),
    TRACKER("/entry-filter/form"),
    REPORT("/reports/all"),
    SUMMARY("/entries/entry-summary-report");

    private final String url;

    RedirectTarget(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public static RedirectTarget fromString(String value) {
        if (value == null) {
            return HOME; // default
        }
        try {
            return RedirectTarget.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return HOME; // default on invalid value
        }
    }


}
