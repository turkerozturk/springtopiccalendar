package com.turkerozturk.dtt.helper;

import com.turkerozturk.dtt.configuration.environment.AppConfigReader;

import java.util.*;

public class SuccessAnalyzer {

    // http://xahlee.info/comp/unicode_circled_numbers.html
    private static final String ONE = "1";
    private static final String ZERO = "0";
    private static final String QUESTION = "?"; // Her ikisi de olabilir, geçiyoruz.
    private static final String ASTERISK = "*"; // en az biri ONE olmalı
    private static final String AMPERSAND = "&"; // en az biri ZERO olmalı
    private static final String PERCENT = "%"; // sadece biri ONE olmalı
    private static final String NUMBER_SIGN = "#"; // sadece biri ZERO olmalı
    private static final String PLUS = "+"; // "+" karakterleri: hepsi ONE ya da hepsi ZERO olmalı
    private static final String DOLLAR_SIGN = "$"; // sadece iki tanesi ONE olmalı
    private static final String AT_SIGN = "@"; // sadece iki tanesi ZERO olmalı
    // bunlar duplicate oldu gibi:
    private static final String CARET_SIGN = "^"; // hepsi ONE olmalı
    private static final String PIPE_SIGN = "|"; // hepsi ZERO olmalı



    public List<Integer> getSuccessArrayNew(
            List<String> rawArray,
            int offsetA,
            Integer offsetB,
            OccurrenceParser occurrenceParser) {

        // System.out.println("rawArray: " + rawArray + ", size: " + rawArray.size());


        int limit = 0;
        if (offsetB == null) {
            limit = rawArray.size();
        } else {
            limit = offsetB + 1;
        }

        List<Integer> reduced = new ArrayList<>();

        int chunkCounterForDebug = 0;
        for (int i = offsetA; i < limit; i += occurrenceParser.getPartitionLength()) {
            List<String> chunk = new ArrayList<>();
            chunkCounterForDebug++;

            for (int j = 0; j < occurrenceParser.getPartitionLength(); j++) {
                int idx = i + j;
                chunk.add(idx < limit ? rawArray.get(idx) : ZERO); // pad with 0
            }

            switch (occurrenceParser.getOccuranceType()) {

                case PATTERNED_FILLED_LOOSE:
                    // Pattern'deki 1'lerin olduğu yerlerde chunk da 1 olmalı, diğer yerlere bakılmaz.
                    boolean matchFilledLoose = true;
                    for (int j = 0; j < occurrenceParser.getOccurancesListInOrder().size(); j++) {
                        if (occurrenceParser.getOccurancesListInOrder().get(j).equals(ONE) && !chunk.get(j).equals(ONE)) {
                            matchFilledLoose = false;
                            break;
                        }
                    }
                    reduced.add(matchFilledLoose ? 1 : 0);
                    break;

                case PATTERNED_EMPTY_LOOSE:
                    // Pattern'deki 0'ların olduğu yerlerde chunk da 0 olmalı, diğer yerlere bakılmaz.
                    boolean matchEmptyLoose = true;
                    for (int j = 0; j < occurrenceParser.getOccurancesListInOrder().size(); j++) {
                        if (occurrenceParser.getOccurancesListInOrder().get(j).equals(ZERO) && !chunk.get(j).equals(ZERO)) {
                            matchEmptyLoose = false;
                            break;
                        }
                    }
                    reduced.add(matchEmptyLoose ? 1 : 0);
                    break;

                case PATTERNED_BOTH_STRICT:
                    // Birebir tüm pattern elemanları chunk ile aynı olmalı.
                    boolean matchBothStrict = true;
                    for (int j = 0; j < occurrenceParser.getOccurancesListInOrder().size(); j++) {
                        if (!chunk.get(j).equals(occurrenceParser.getOccurancesListInOrder().get(j))) {
                            matchBothStrict = false;
                            break;
                        }
                    }
                    reduced.add(matchBothStrict ? 1 : 0);
                    break;
                case PATTERNED_SEMI_STRICT:
                    boolean matchSemiStrict = true;
                    List<String> pattern = occurrenceParser.getOccurancesListInOrder();
                    for (int j = 0; j < pattern.size(); j++) {
                        String p = pattern.get(j);
                        String c = chunk.get(j);


                        if(p.equals(ONE) && !c.equals(ONE)) {
                            matchSemiStrict = false;
                        } else if(p.equals(ZERO) && !c.equals(ZERO)) {
                            matchSemiStrict = false;
                        } else if (p.equals(QUESTION)) { // one veya zero olabilir

                        }
                    }
                    reduced.add(matchSemiStrict ? 1 : 0);
                    break;
                case PATTERNED_ADVANCED:
                    boolean matchAdvanced = true;
                    List<String> advancedPattern = occurrenceParser.getOccurancesListInOrder();

                    List<Integer> plusIndexes = new ArrayList<>();
                    List<Integer> asteriskIndexes = new ArrayList<>();
                    List<Integer> ampersandIndexes = new ArrayList<>();
                    List<Integer> percentIndexes = new ArrayList<>();
                    List<Integer> numberSignIndexes = new ArrayList<>();
                    List<Integer> dollarSignIndexes = new ArrayList<>();
                    List<Integer> atSignIndexes = new ArrayList<>();
                    List<Integer> caretSignIndexes = new ArrayList<>();
                    List<Integer> pipeSignIndexes = new ArrayList<>();

                    for (int j = 0; j < advancedPattern.size(); j++) {
                        String p = advancedPattern.get(j);
                        String c = chunk.get(j);


                        switch (p) {
                            case ONE:
                         //       System.out.println(ONE + ": " + j + ". " + c + " (" + p + ")");
                                if (!c.equals(ONE)) matchAdvanced = false;
                                break;
                            case ZERO:
                         //       System.out.println(ONE + ": " + j + ". " + c + " (" + p + ")");

                                if (!c.equals(ZERO)) matchAdvanced = false;
                                break;
                            case QUESTION:
                         //       System.out.println(ONE + ": " + j + ". " + c + " (" + p + ")");

                                // Her ikisi de olabilir, geçiyoruz.
                                break;
                            case PLUS:
                                plusIndexes.add(j); // sonra topluca kontrol edeceğiz
                                break;
                            case ASTERISK:
                                asteriskIndexes.add(j); // sonra topluca kontrol edeceğiz
                                break;
                            case AMPERSAND:
                                ampersandIndexes.add(j);
                                break;
                            case PERCENT:
                                percentIndexes.add(j);
                                break;
                            case NUMBER_SIGN:
                                numberSignIndexes.add(j);
                                break;
                            case DOLLAR_SIGN:
                                dollarSignIndexes.add(j);
                                break;
                            case AT_SIGN:
                                atSignIndexes.add(j);
                                break;
                            case CARET_SIGN:
                                caretSignIndexes.add(j);
                                break;
                            case PIPE_SIGN:
                                pipeSignIndexes.add(j);
                                break;
                            default:
                                matchAdvanced = false; // tanımsız karakter varsa eşleşme olmasın
                        }
                    }
                   // System.out.println("&: " + ampersandIndexes);
                   // System.out.println("+ : " + plusIndexes);
                   // System.out.println("* " + asteriskIndexes);

                    // "+" karakterleri: hepsi ONE ya da hepsi ZERO olmalı

                    if (!plusIndexes.isEmpty()) {
                        boolean allOne = true;
                        boolean allZero = true;
                        for (int ii : plusIndexes) {
                            String c = chunk.get(ii);
                            if (!c.equals(ONE)) allOne = false;
                            if (!c.equals(ZERO)) allZero = false;
                        }
                        if (!allOne && !allZero) matchAdvanced = false;
                    }



                    // "&" karakterleri: en az biri ZERO olmalı

                    if (!ampersandIndexes.isEmpty()) {
                        boolean hasZero = false;
                        for (int iii : ampersandIndexes) {
                            if (chunk.get(iii).equals(ZERO)) {
                                hasZero = true;
                                break;
                            }
                        }
                        if (!hasZero) matchAdvanced = false;
                    }



                    // "*" karakterleri: en az biri ONE olmalı
                    if (!asteriskIndexes.isEmpty()) {
                        boolean hasOne = false;
                        for (int iiii : asteriskIndexes) {
                            if (chunk.get(iiii).equals(ONE)) {
                                hasOne = true;
                                break;
                            }
                        }
                        if (!hasOne) matchAdvanced = false;
                    }

                    // % => sadece biri ONE olmalı
                    if (!percentIndexes.isEmpty()) {
                        int countOne_p = 0;
                        for (int j1 : percentIndexes) {
                            if (chunk.get(j1).equals(ONE)) {
                                countOne_p++;
                            }
                        }
                        if (countOne_p != 1) matchAdvanced = false;
                    }

                    // # => sadece biri ZERO olmalı
                    if (!numberSignIndexes.isEmpty()) {
                        int countZero_h = 0;
                        for (int j2 : numberSignIndexes) {
                            if (chunk.get(j2).equals(ZERO)) {
                                countZero_h++;
                            }
                        }
                        if (countZero_h != 1) matchAdvanced = false;
                    }

                    // $ => sadece iki tanesi ONE olmalı
                    if (!dollarSignIndexes.isEmpty()) {
                        int countOne_d = 0;
                        for (int j3 : dollarSignIndexes) {
                            if (chunk.get(j3).equals(ONE)) {
                                countOne_d++;
                            }
                        }
                        if (countOne_d != 2) matchAdvanced = false;
                    }

                    // @ => sadece iki tanesi ZERO olmalı
                    if (!atSignIndexes.isEmpty()) {
                        int countZero_a = 0;
                        for (int j4 : atSignIndexes) {
                            if (chunk.get(j4).equals(ZERO)) {
                                countZero_a++;
                            }
                        }
                        if (countZero_a != 2) matchAdvanced = false;
                    }

                    // ^ => hepsi ONE olmalı
                    if (!caretSignIndexes.isEmpty()) {
                        boolean allOne_caret = true;
                        for (int j5 : caretSignIndexes) {
                            if (!chunk.get(j5).equals(ONE)) {
                                allOne_caret = false;
                                break;
                            }
                        }
                        if (!allOne_caret) matchAdvanced = false;
                    }

                    // | => hepsi ZERO olmalı
                    if (!pipeSignIndexes.isEmpty()) {
                        boolean allZero_pipe = true;
                        for (int j6 : pipeSignIndexes) {
                            if (!chunk.get(j6).equals(ZERO)) {
                                allZero_pipe = false;
                                break;
                            }
                        }
                        if (!allZero_pipe) matchAdvanced = false;
                    }



                    reduced.add(matchAdvanced ? 1 : 0);
                    break;

                case ONE_FILLED_IN_ONE:
                    // Pattern length zaten 1, chunk da sadece 1 varsa match
                    reduced.add((chunk.size() == 1 && chunk.get(0).equals(ONE)) ? 1 : 0);
                    break;

                case RANDOM_FILLED_LOOSE:
                    // Chunk içindeki 1 sayısı istenen sayıdan fazla veya eşitse match
                    long countOnesLoose = chunk.stream().filter(x -> x.equals(ONE)).count();
                    reduced.add(countOnesLoose >= occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case RANDOM_FILLED_STRICT:
                    // 1 sayısı tam olarak belirtilen sayıya eşitse match
                    long countOnesStrict = chunk.stream().filter(x -> x.equals(ONE)).count();
                    reduced.add(countOnesStrict == occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case ONE_EMPTY_IN_ONE:
                    // Pattern length zaten 1, chunk da sadece 0 varsa match
                    reduced.add((chunk.size() == 1 && chunk.get(0).equals(ZERO)) ? 1 : 0);
                    break;

                case RANDOM_EMPTY_LOOSE:
                    // 0 sayısı istenen sayıdan fazla veya eşitse match
                    long countZerosLoose = chunk.stream().filter(x -> x.equals(ZERO)).count();
                    reduced.add(countZerosLoose >= occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case RANDOM_EMPTY_STRICT:
                    // 0 sayısı tam olarak belirtilen sayıya eşitse match
                    long countZerosStrict = chunk.stream().filter(x -> x.equals(ZERO)).count();
                    reduced.add(countZerosStrict == occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case ALL_FILLED:
                    // Tüm elemanlar 1 ise match
                    boolean allOnes = chunk.stream().allMatch(x -> x.equals(ONE));
                    reduced.add(allOnes ? 1 : 0);
                    break;

                case ALL_EMPTY:
                    // Tüm elemanlar 0 ise match
                    boolean allZeros = chunk.stream().allMatch(x -> x.equals(ZERO));
                    reduced.add(allZeros ? 1 : 0);
                    break;

                default:
                    throw new IllegalArgumentException("Either randomOccuranceCount or occurancesListInOrder must be provided.");
            }

            if (AppConfigReader.isDebugIntervalRuleEnabled()) {
                System.out.println(String.format("%03d", chunkCounterForDebug) + "\t" +
                        (reduced.get(reduced.size() - 1) == 1 ? "match" : "-----") + "\t" +
                        String.format("%03d", i + 1) + "\t" +
                        chunk);
            }

        }

        return reduced;
    }



    public static double getSuccessRate(List<Integer> reducedArray) {
        if (reducedArray.isEmpty()) return 0.0;
        long successCount = getSuccessCount(reducedArray);
        return (double) successCount / reducedArray.size();
    }

    public static long getSuccessCount(List<Integer> reducedArray) {
        if (reducedArray.isEmpty()) return 0;
        long successCount = reducedArray.stream().filter(x -> x == 1).count();
        return successCount;
    }

    public static String getNextStatus(List<Integer> reducedArray) {
        int n = reducedArray.size();
        if (n < 2) return "?";

        int last = reducedArray.get(n - 1);
        int prev = reducedArray.get(n - 2);

        long ones = reducedArray.stream().filter(i -> i == 1).count();
        long zeros = reducedArray.size() - ones;

        if (ones > zeros && last == 1 && prev == 1) return ONE;
        else if (zeros > ones && last == 0 && prev == 0) return ZERO;
        else if (ones > zeros) return "+";
        else if (zeros > ones) return "-";
        else return "~"; // eşit ya da belirsiz
    }

    /*
    // === Test ===
    public static void main(String[] args) {
        List<Integer> rawArray = Arrays.asList(0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0);

        // Test 1: OccuranceCount
        List<Integer> reduced1 = getSuccessArray(rawArray, 0, 3, 1, null);
        System.out.println("Reduced1: " + reduced1);
        System.out.printf("Success Rate: %.2f%%\n", getSuccessRate(reduced1) * 100);
        System.out.println("Next Status: " + getNextStatus(reduced1));

        // Test 2: OccuranceOrder
        List<Integer> occurancePattern = Arrays.asList(0,1,0);
        List<Integer> reduced2 = getSuccessArray(rawArray, 0, 3, null, occurancePattern);
        System.out.println("Reduced2: " + reduced2);
        System.out.printf("Success Rate: %.2f%%\n", getSuccessRate(reduced2) * 100);
        System.out.println("Next Status: " + getNextStatus(reduced2));
    }

    */

    public static Integer findFirstDoneOffsetOfArray(List<String> array) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).equals(ONE)) {
                return i;
            }
        }
        return null;
    }


    /*
    public static List<Integer> convertEntriesMapToRawArray(Map<Long, Entry> entriesMap) {
        List<Integer> rawArray = new ArrayList<>();

        for (Long key : entriesMap.keySet()) {
            Entry entry = entriesMap.get(key);
            if (entry == null) {
                rawArray.add(0);
            } else {
                rawArray.add(entry.getStatus() == 1 ? 1 : 0);
            }
        }

        return rawArray;
    }
    */

}
