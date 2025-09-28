package com.turkerozturk.dtt.component;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.jsoup.nodes.Document;
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

        // linklere target="_blank" ekle
        Document jsoupDoc = Jsoup.parse(unsafeHtml);
        // zaten markdownda target blank eklemek olsa da pek konforlu degil, yorumlarda a href olarak eklenmesi tavsiye ediliyor.
        // biz de hic markdownla ugrasmadan, ciktiyi verirken target blank ekliyoruz ve cok guzel bir cozum oldu.
        jsoupDoc.select("a[href]").forEach(a -> a.attr("target", "_blank"));

        // HTML'i temizle (XSS korumasi)
        String safeHtml = Jsoup.clean(jsoupDoc.body().html(),
                Safelist.basicWithImages().addAttributes("a", "target", "rel")); // güvenlik için rel de eklenebilir


        return safeHtml;

    }
}

