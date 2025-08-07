package com.turkerozturk.dtt.helper;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class OccurrenceParser {

    private Integer partitionLength;
    private Integer randomOccuranceCount;
    private List<String> occurancesListInOrder;
    private OccuranceType occuranceType;

    private boolean isDisabled; // sinif degiskeni olmasinin sebebi frontend'de bilgi amacli gostermek icin.
    private boolean isStrict; // sinif degiskeni olmasinin sebebi frontend'de bilgi amacli gostermek icin.
    private boolean isOpposite; // sinif degiskeni olmasinin sebebi frontend'de bilgi amacli gostermek icin.
    private String inputExtended; // sinif degiskeni olmasinin sebebi frontend'de bilgi amacli gostermek icin.

    public void parse(String inputExtended) {
        this.inputExtended = inputExtended;
        if (inputExtended == null || inputExtended.isBlank()) {
            getDefaultResult();
            return;
        }

        String temporaryString = inputExtended;
        isDisabled = false;
        isStrict = false;
        isOpposite = false;
        String input = null;

        // 1. Başta ' varsa disable et ve baştaki ' karakterini sil
        if (temporaryString.startsWith("'")) {
            isDisabled = true;
            temporaryString = temporaryString.substring(1);
            getDefaultResult();
            return; // varsayilan degerlerleri yani gunde bir kez' atayip geri donuyoruz cunku intervalRule disabled durumda.
        }

        // 2. İlk rakam karakterinin indeksini bul
        int firstDigitIndex = -1;
        for (int i = 0; i < temporaryString.length(); i++) {
            if (Character.isDigit(temporaryString.charAt(i))) {
                firstDigitIndex = i;
                break;
            }
        }

        // 3. Eğer rakamdan önce '!' varsa isStrict = true ve sil
        if (firstDigitIndex > 0) {
            int strictIndex = temporaryString.indexOf('!');
            if (strictIndex != -1 && strictIndex < firstDigitIndex) {
                isStrict = true;
                temporaryString = temporaryString.substring(0, strictIndex)
                        + temporaryString.substring(strictIndex + 1);
            }
        }

        // 4. Eğer rakamdan önce '-' varsa isOpposite = true ve sil
        if (firstDigitIndex > 0) {
            int oppositeIndex = temporaryString.indexOf('-');
            if (oppositeIndex != -1 && oppositeIndex < firstDigitIndex) {
                isOpposite = true;
                temporaryString = temporaryString.substring(0, oppositeIndex)
                        + temporaryString.substring(oppositeIndex + 1);
            }
        }

        // 5. Geriye kalan input string
        input = temporaryString.trim();

        if (input == null || input.isBlank()) {
            getDefaultResult();
        } else {

            input = input.trim();


            // Format 3: Comma-separated list
            if (!input.contains("/")) {
                List<String> orderList = new ArrayList<>();
                String[] parts = input.split("");

                for (String part : parts) {
                    try {
                        orderList.add(part.trim());
                    } catch (NumberFormatException e) {
                        getDefaultResult(); // Invalid entry
                    }
                }
                setPartitionLength(orderList.size());
                setRandomOccuranceCount(null);
                setOccurancesListInOrder(orderList);

                setOccuranceType(OccuranceType.PATTERNED_ADVANCED);

                /*
                if (isStrict) {
                    //setOccuranceType(OccuranceType.PATTERNED_BOTH_STRICT);
                    setOccuranceType(OccuranceType.PATTERNED_ADVANCED);

                } else {
                    if (isOpposite) {
                        setOccuranceType(OccuranceType.PATTERNED_EMPTY_LOOSE);
                    } else {
                       // setOccuranceType(OccuranceType.PATTERNED_FILLED_LOOSE);
                        setOccuranceType(OccuranceType.PATTERNED_SEMI_STRICT);

                    }
                }
                */
            } else if (input.contains("/")) { // Format 2: a/b
                String[] parts = input.split("/");
                if (parts.length == 2) {
                    try {
                        setPartitionLength(Integer.parseInt(parts[1].trim()));
                        setRandomOccuranceCount(Integer.parseInt(parts[0].trim()));
                        setOccurancesListInOrder(null);
                        if (isStrict) {
                            if (isOpposite) {
                                setOccuranceType(OccuranceType.RANDOM_EMPTY_STRICT);
                            } else {
                                setOccuranceType(OccuranceType.RANDOM_FILLED_STRICT);
                            }
                        } else {
                            if (isOpposite) {
                                setOccuranceType(OccuranceType.RANDOM_EMPTY_LOOSE);
                            } else {
                                setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
                            }
                        }
                    } catch (NumberFormatException e) {
                        getDefaultResult();
                    }
                } else {
                    getDefaultResult(); // Invalid a/b format
                }
            } else {

                // Format 1: single integer
                try {
                    setPartitionLength(Integer.parseInt(input));
                    setRandomOccuranceCount(1);
                    setOccurancesListInOrder(null);
                    if (isStrict) {
                        if (isOpposite) {
                            setOccuranceType(OccuranceType.RANDOM_EMPTY_STRICT);
                        } else {
                            setOccuranceType(OccuranceType.RANDOM_FILLED_STRICT);
                        }
                    } else {
                        if (isOpposite) {
                            setOccuranceType(OccuranceType.RANDOM_EMPTY_LOOSE);
                        } else {
                            setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
                        }
                    }
                } catch (NumberFormatException e) {
                    getDefaultResult();
                }
            }
        }
    }

    // varsayilan degerler, eger bir kural yoksa veya hata varsa veya hicbir kurala uymuyorsa veya disabled ise bu degerler atanir.
    private void getDefaultResult() {
        setPartitionLength(1);
        setRandomOccuranceCount(1);
        setOccurancesListInOrder(null);
        setOccuranceType(OccuranceType.ONE_FILLED_IN_ONE);
    }

    private static final String YES = "Yes";
    private static final String NO = "No";

    public String toHTML() {
        StringBuilder sb = new StringBuilder();

        sb.append("<table style=\"border: solid 1px; text-align:left;width:100%;\">\n");


        sb.append("<tr><td colspan=\"2\" ")
                .append("style=\"font-weight:bold; text-align:center;\" ")
                .append("title=\"")
                .append(inputExtended != null ? inputExtended : "")
                .append("\"")
                .append(">Interval Rule</td></tr>\n");


        sb.append("<tr><td title=\"isDisabled\">Active?</td><td>")
                .append(!isDisabled ? YES : NO)
                .append("</td></tr>\n");

        // Normal değişkenler: 2 sütunlu satırlar
        sb.append("<tr><td title=\"partitionLength\">Length</td><td>")
                .append(partitionLength != null ? partitionLength : "")
                .append("</td></tr>\n");

        sb.append("<tr><td colspan=\"2\" ")
                .append("style=\"font-weight:bold; text-align:center;\" ")
                .append("title=\"")
                .append(occuranceType != null ? occuranceType.toString() : "")
                .append("\"")
                .append(">Occurance Type</td></tr>\n");

         if (isDisabled) {
            sb.append("<tr><td colspan=\"2\">\n")
                    .append("<span>The existing interval rule is disabled. You can enable it by removing the ' character at the beginning of the rule text. The default rule is daily.</span>")
                    .append("</td></tr>\n");
        } else if (occuranceType.equals(OccuranceType.PATTERNED_BOTH_STRICT) ||
                occuranceType.equals(OccuranceType.PATTERNED_FILLED_LOOSE) ||
                occuranceType.equals(OccuranceType.PATTERNED_EMPTY_LOOSE) ||
                 occuranceType.equals(OccuranceType.PATTERNED_SEMI_STRICT) ||
                         occuranceType.equals(OccuranceType.PATTERNED_ADVANCED) ) {

            // Special case: occurancesListInOrder
            sb.append("<tr><td colspan=\"2\" style=\"font-weight:bold; text-align:center;\">Pattern</td></tr>\n");

            sb.append("<tr><td colspan=\"2\">\n");
            sb.append("<table style=\"border-collapse: collapse;margin:auto;\">\n");

            int tableRowDivider = 14;
            if (occurancesListInOrder != null) {
                for (int i = 0; i < occurancesListInOrder.size(); i++) {
                    if (i % tableRowDivider == 0) {
                        sb.append("<tr>\n");
                    }

                    String value = occurancesListInOrder.get(i);
                    String bgColor = "white";
                    if(value.equals("1")) {
                        bgColor = "PaleGreen";
                    } else if(value.equals("?")) {
                        bgColor = "Yellow";
                    }

                    sb.append("<td style=\"width:15px; height:15px; text-align:center; background-color:")
                            .append(bgColor)
                            .append("; border:1px solid #ccc; font-size:10px;\">")
                            .append(value)
                            .append("</td>\n");

                    if (i % tableRowDivider == (tableRowDivider - 1) || i == occurancesListInOrder.size() - 1) {
                        sb.append("</tr>\n");
                    }
                }
            }

            sb.append("</table>\n");
            sb.append("</td></tr>\n");


        } else if (occuranceType.equals(OccuranceType.RANDOM_FILLED_LOOSE) ||
                occuranceType.equals(OccuranceType.RANDOM_FILLED_STRICT) ||
                occuranceType.equals(OccuranceType.RANDOM_EMPTY_STRICT) ||
                occuranceType.equals(OccuranceType.RANDOM_EMPTY_LOOSE) ) {

             sb.append("<tr><td title=\"inputExtended\">Rule</td><td>")
                     .append(inputExtended != null ? inputExtended : "")
                     .append("</td></tr>\n");

            sb.append("<tr><td title=\"randomOccuranceCount\">Random Count</td><td>")
                    .append(randomOccuranceCount != null ? randomOccuranceCount : "")
                    .append("</td></tr>\n");

        } else if (occuranceType.equals(OccuranceType.ONE_FILLED_IN_ONE) ||
                occuranceType.equals(OccuranceType.ONE_EMPTY_IN_ONE)  ) {

            sb.append("<tr><td title=\"randomOccuranceCount\">Random Count</td><td>")
                    .append(randomOccuranceCount != null ? randomOccuranceCount : "")
                    .append("</td></tr>\n");

        } else {
            sb.append("<tr><td colspan=\"2\">\n")
                    .append("<span>The interval rule is not set. The default rule is daily.</span>")
                    .append("</td></tr>\n");
        }

       // "The default rule is daily. Days marked as \"done\" are considered.";

        if(!isDisabled) {
            sb.append("<tr><td title=\"isStrict\">Strict?")
                    .append(isStrict == true ? " <b class=\"blink\">!</b>" : "")
                    .append("</td><td>")
                    .append(isStrict == true ? YES : NO)
                    .append("</td></tr>\n");

            sb.append("<tr><td title=\"isOpposite\">Opposite?")
                    .append(isOpposite == true ? " <b class=\"blink\">-</b>" : "")
                    .append("</td><td>")
                    .append(isOpposite == true ? YES : NO)
                    .append("</td></tr>\n");

        }


        sb.append("</table>");

        return sb.toString();
    }



    /*
    // Test
    public static void main(String[] args) {
        List<String> testInputs = Arrays.asList("2", "3/5", "0,1,0,1,0,0", "abc", null, "4/xyz");

        OccurrenceParser occurrenceParser = new OccurrenceParser();

        for (String input : testInputs) {
            occurrenceParser.parse(input);
            System.out.println(occurrenceParser + " <- " + "Raw Input: " + input);
        }
    }
    */


}
