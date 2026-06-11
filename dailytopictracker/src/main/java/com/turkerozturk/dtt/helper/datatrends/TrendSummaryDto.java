package com.turkerozturk.dtt.helper.datatrends;

import lombok.Data;

@Data
public class TrendSummaryDto {

    private long upCount;

    private long downCount;

    private long sameCount;

    // https://unicode-explorer.com/list/arrows

    public String getDisplayText3() {
        return htmlize(String.format("%03d", downCount),-1) + SEPARATOR3 // 🠟
                + htmlize(String.format("%03d", sameCount), 0) + SEPARATOR3 // =
                + htmlize(String.format("%03d", upCount),1); //🠝
    }

    public String getDisplayText4() {
        return downCount + htmlize("\uD83E\uDC1F",-1) + SEPARATOR // 🠟
                + sameCount+ SEPARATOR // =
                + upCount+ htmlize("\uD83E\uDC1D",1); //🠝
    }

    public String getDisplayText2() {
        return downCount + htmlize("↓",-1) + SEPARATOR
                + sameCount+ SEPARATOR // =
                + upCount+ htmlize("↑",1);
    }

    public String getDisplayText() {
        return downCount + htmlize("\uD83E\uDC66",-1) + SEPARATOR // 🡦
                + sameCount+ SEPARATOR // =
                + upCount + htmlize("\uD83E\uDC65",1); //  🡥
    }

    private static final String SEPARATOR = ",";
    private static final String SEPARATOR3 = "";

    private static String htmlize(String value, int direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("<span style=\"");
        switch (direction) {
            case -1 -> sb.append(cssTrendArrowDown);
            case 0 -> sb.append(cssTrendArrowSame);
            case 1 -> sb.append(cssTrendArrowUp);
        }
        sb.append("\">");
        sb.append(value);
        sb.append("</span>");


        return sb.toString();
    }

    // html headerdaki CSS yi uygulamadigi icin burada inline css olarak tanimlamak gerekti.
    private static final String cssTrendArrowUp = "background-color: salmon; color: cyan; border-style: solid; border-color: black; border-width:1px; font-weight: bold;";
    private static final String cssTrendArrowDown = "background-color: powderblue; color: salmon; border-style: solid; border-color: black; border-width:1px; font-weight: bold;";
    private static final String cssTrendArrowSame = "background-color: green; color: yellow; border-style: solid; border-color: black; border-width:1px; font-weight: normal;";


}
