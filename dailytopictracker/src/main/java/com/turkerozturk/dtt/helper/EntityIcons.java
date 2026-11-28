package com.turkerozturk.dtt.helper;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class EntityIcons {
    public static final String ENABLED_KEY = "ui.entityIcons.enabled";

    // The bundled Font Awesome Free solid webfont, without brand glyphs.
    private static final Map<String, String> CATALOG = buildCatalog();

    private EntityIcons() {
    }

    public static Map<String, String> catalog() {
        return CATALOG;
    }

    public static String selection(String current, String submitted) {
        if (submitted == null) return current;
        if (submitted.isEmpty()) return null;
        if (!CATALOG.containsKey(submitted)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown icon");
        }
        return submitted;
    }

    private static Map<String, String> buildCatalog() {
        String keys = """
                apple-whole archive arrow-trend-up award baby bag-shopping basket-shopping
                bed bell bicycle bolt book book-open bookmark box box-archive briefcase broom building
                bullseye burger bus calendar calendar-check camera campground car cart-shopping cat
                chart-column chart-line check check-double circle-check clipboard clock cloud code coffee
                coins compass computer dog dumbbell envelope feather file file-lines film fire
                fish flag flask folder folder-open football gamepad gift globe graduation-cap
                guitar hammer hand-holding-heart headphones heart heart-pulse house image inbox industry
                key keyboard laptop leaf lightbulb list list-check location-dot lock map map-pin
                martini-glass microphone mobile-screen moon mountain mug-hot music newspaper note-sticky
                palette paper-plane pen-to-square pencil person-hiking person-running phone pills plane
                plant-wilt plug puzzle-piece recycle ribbon road rocket route seedling server
                shield-halved shoe-prints shop snowflake futbol spoon star suitcase sun tag tags
                thermometer thumbs-up ticket tree trophy truck umbrella user users utensils wallet
                water weight-scale wifi wrench
                """;
        var icons = new TreeMap<String, String>();
        Arrays.stream(keys.strip().split("\\s+")).forEach(key -> {
            String label = key.replace('-', ' ');
            icons.put(key, Character.toUpperCase(label.charAt(0)) + label.substring(1));
        });
        return Collections.unmodifiableMap(icons);
    }
}
