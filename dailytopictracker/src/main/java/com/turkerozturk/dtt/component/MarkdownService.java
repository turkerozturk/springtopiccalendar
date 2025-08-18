package com.turkerozturk.dtt.component;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Component
public class MarkdownService {
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";

        // Markdown -> HTML
        Node document = parser.parse(markdown);
        String unsafeHtml = renderer.render(document);

        // HTML'i temizle (XSS korumasi)
        String safeHtml = Jsoup.clean(unsafeHtml, Safelist.basicWithImages());

        return safeHtml;
    }
}

