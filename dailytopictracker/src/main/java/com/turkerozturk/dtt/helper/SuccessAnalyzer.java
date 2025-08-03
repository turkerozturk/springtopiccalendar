package com.turkerozturk.dtt.helper;

import java.util.*;

public class SuccessAnalyzer {


    public List<Integer> getSuccessArrayNew(
            List<Integer> rawArray,
            int offsetA,
            Integer offsetB,
            OccurrenceParser occurrenceParser) {

        // System.out.println("rawArray: " + rawArray + ", size: " + rawArray.size());
        List<Integer> reduced = new ArrayList<>();

        int limit = 0;
        if (offsetB == null) {
            limit = rawArray.size();
        } else {
            limit = offsetB + 1;
        }

        for (int i = offsetA; i < limit; i += occurrenceParser.getPartitionLength()) {

            List<Integer> chunk = new ArrayList<>();

            for (int j = 0; j < occurrenceParser.getPartitionLength(); j++) {
                int idx = i + j;
                chunk.add(idx < limit ? rawArray.get(idx) : 0); // pad with 0
            }

            //
            switch (occurrenceParser.getOccuranceType()) {

                case PATTERNED_FILLED_LOOSE:
                    // match olması için pattern'deki 1'lerin yeri aynı olmalı, diğerleri 0 veya 1 olsa da farketmez.
                    boolean match = true;
                    for (int j = 0; j < occurrenceParser.getOccurancesListInOrder().size(); j++) {
                        if (occurrenceParser.getOccurancesListInOrder().get(j) == 1 && chunk.get(j) != 1) {
                            match = false;
                            break;
                        }
                    }
                    reduced.add(match ? 1 : 0);
                    break;
                case PATTERNED_EMPTY_LOOSE:
                    // TODO match olması için pattern'deki sıfırların yeri aynı olmalı, diğerleri 0 veya 1 olsa da farketmez.
                    break;
                case PATTERNED_BOTH_STRICT:
                    // TODO match olması için pattern'deki 1'lerin ve sıfırların yeri birebir aynı olmalı.
                    break;

                case ONE_FILLED_IN_ONE:
                    // 1 tane bir var, pattern length de 1
                case RANDOM_FILLED_LOOSE:
                    // 1'lerin sayısı belirtilen kadar veya daha fazla olabilir.
                    long countOnes = chunk.stream().filter(x -> x == 1).count();
                    reduced.add(countOnes >= occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;
                case RANDOM_FILLED_STRICT:
                    // TODO 1'lerin sayısı aynı olmalı
                    break;

                case ONE_EMPTY_IN_ONE:
                    // 1 tane sıfır var, pattern length de 1
                case RANDOM_EMPTY_LOOSE:
                    // TODO 0'lerin sayısı belirtilen kadar veya daha fazla olabilir.
                    break;
                case RANDOM_EMPTY_STRICT:
                    // TODO 0'ların sayısı aynı olmalı
                    break;

                case ALL_FILLED:
                    // TODO hepsi 1 olmalı
                    break;
                case ALL_EMPTY:
                    // TODO hepsi 0 olmalı
                    break;
                default:
                    throw new IllegalArgumentException("Either randomOccuranceCount or occurancesListInOrder must be provided.");
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

        if (ones > zeros && last == 1 && prev == 1) return "1";
        else if (zeros > ones && last == 0 && prev == 0) return "0";
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

    public static Integer findFirstDoneOffsetOfArray(List<Integer> array) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) == 1) {
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
