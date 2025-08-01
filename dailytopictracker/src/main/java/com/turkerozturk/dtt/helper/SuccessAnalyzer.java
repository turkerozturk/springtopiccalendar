package com.turkerozturk.dtt.helper;

import java.util.*;

public class SuccessAnalyzer {

    public static List<Integer> getSuccessArray(
            List<Integer> rawArray,
            int offset,
            Integer offsetB,
            int successDivider,
            Integer occuranceCount,
            List<Integer> occuranceOrder) {
        // System.out.println("rawArray: " + rawArray + ", size: " + rawArray.size());
        List<Integer> reduced = new ArrayList<>();
        int limit = 0;
        if (offsetB == null) {

            limit = rawArray.size();
        } else {
            limit = offsetB + 1;
        }

        for (int i = offset; i < limit; i += successDivider) {

            if (successDivider <= 0) {
                throw new IllegalArgumentException("successDivider must be > 0");
            }
            if (occuranceCount == null && (occuranceOrder == null || occuranceOrder.isEmpty())) {
                throw new IllegalArgumentException("Either occuranceCount or occuranceOrder must be provided.");
            }


            List<Integer> chunk = new ArrayList<>();

            for (int j = 0; j < successDivider; j++) {
                int idx = i + j;
                chunk.add(idx < limit ? rawArray.get(idx) : 0); // pad with 0
            }

            // Mode 1: occuranceCount ile karar ver
            if (occuranceCount != null) {
                long countOnes = chunk.stream().filter(x -> x == 1).count();
                reduced.add(countOnes >= occuranceCount ? 1 : 0);
            }
            // Mode 2: occuranceOrder ile karar ver
            else if (occuranceOrder != null && !occuranceOrder.isEmpty()) {
                boolean match = true;
                for (int j = 0; j < Math.min(successDivider, occuranceOrder.size()); j++) {
                    if (occuranceOrder.get(j) == 1 && chunk.get(j) != 1) {
                        match = false;
                        break;
                    }
                }
                reduced.add(match ? 1 : 0);
            } else {
                throw new IllegalArgumentException("Either occuranceCount or occuranceOrder must be provided.");
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
