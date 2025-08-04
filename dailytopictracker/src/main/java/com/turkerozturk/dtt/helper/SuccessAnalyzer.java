package com.turkerozturk.dtt.helper;

import java.util.*;

public class SuccessAnalyzer {


    public List<Integer> getSuccessArrayNew(
            List<Integer> rawArray,
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

        for (int i = offsetA; i < limit; i += occurrenceParser.getPartitionLength()) {
            List<Integer> chunk = new ArrayList<>();

            for (int j = 0; j < occurrenceParser.getPartitionLength(); j++) {
                int idx = i + j;
                chunk.add(idx < limit ? rawArray.get(idx) : 0); // pad with 0
            }

            switch (occurrenceParser.getOccuranceType()) {

                case PATTERNED_FILLED_LOOSE:
                    // Pattern'deki 1'lerin olduğu yerlerde chunk da 1 olmalı, diğer yerlere bakılmaz.
                    boolean matchFilledLoose = true;
                    for (int j = 0; j < occurrenceParser.getOccurancesListInOrder().size(); j++) {
                        if (occurrenceParser.getOccurancesListInOrder().get(j) == 1 && chunk.get(j) != 1) {
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
                        if (occurrenceParser.getOccurancesListInOrder().get(j) == 0 && chunk.get(j) != 0) {
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

                case ONE_FILLED_IN_ONE:
                    // Pattern length zaten 1, chunk da sadece 1 varsa match
                    reduced.add((chunk.size() == 1 && chunk.get(0) == 1) ? 1 : 0);
                    break;

                case RANDOM_FILLED_LOOSE:
                    // Chunk içindeki 1 sayısı istenen sayıdan fazla veya eşitse match
                    long countOnesLoose = chunk.stream().filter(x -> x == 1).count();
                    reduced.add(countOnesLoose >= occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case RANDOM_FILLED_STRICT:
                    // 1 sayısı tam olarak belirtilen sayıya eşitse match
                    long countOnesStrict = chunk.stream().filter(x -> x == 1).count();
                    reduced.add(countOnesStrict == occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case ONE_EMPTY_IN_ONE:
                    // Pattern length zaten 1, chunk da sadece 0 varsa match
                    reduced.add((chunk.size() == 1 && chunk.get(0) == 0) ? 1 : 0);
                    break;

                case RANDOM_EMPTY_LOOSE:
                    // 0 sayısı istenen sayıdan fazla veya eşitse match
                    long countZerosLoose = chunk.stream().filter(x -> x == 0).count();
                    reduced.add(countZerosLoose >= occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case RANDOM_EMPTY_STRICT:
                    // 0 sayısı tam olarak belirtilen sayıya eşitse match
                    long countZerosStrict = chunk.stream().filter(x -> x == 0).count();
                    reduced.add(countZerosStrict == occurrenceParser.getRandomOccuranceCount() ? 1 : 0);
                    break;

                case ALL_FILLED:
                    // Tüm elemanlar 1 ise match
                    boolean allOnes = chunk.stream().allMatch(x -> x == 1);
                    reduced.add(allOnes ? 1 : 0);
                    break;

                case ALL_EMPTY:
                    // Tüm elemanlar 0 ise match
                    boolean allZeros = chunk.stream().allMatch(x -> x == 0);
                    reduced.add(allZeros ? 1 : 0);
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
