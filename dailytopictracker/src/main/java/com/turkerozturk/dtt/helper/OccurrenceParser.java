package com.turkerozturk.dtt.helper;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class OccurrenceParser {

    private Integer partitionLength;
    private Integer randomOccuranceCount;
    private List<Integer> occurancesListInOrder;
    private OccuranceType occuranceType;

    public void parse(String input) {


        if (input == null || input.isBlank()) {
            getDefaultResult();
        } else {


            input = input.trim();

            // Format 3: Comma-separated list
            if (input.contains(",")) {
                List<Integer> orderList = new ArrayList<>();
                String[] parts = input.split(",");
                for (String part : parts) {
                    try {
                        orderList.add(Integer.parseInt(part.trim()));
                    } catch (NumberFormatException e) {
                        getDefaultResult(); // Invalid entry
                    }
                }
                setPartitionLength(orderList.size());
                setRandomOccuranceCount(null);
                setOccurancesListInOrder(orderList);
                setOccuranceType(OccuranceType.PATTERNED_FILLED_LOOSE);
            } else if (input.contains("/")) { // Format 2: a/b
                String[] parts = input.split("/");
                if (parts.length == 2) {
                    try {
                        setPartitionLength(Integer.parseInt(parts[1].trim()));
                        setRandomOccuranceCount(Integer.parseInt(parts[0].trim()));
                        setOccurancesListInOrder(null);
                        setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
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
                    setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
                } catch (NumberFormatException e) {
                    getDefaultResult();
                }
            }
        }
    }

    private void getDefaultResult() {
        setPartitionLength(1);
        setRandomOccuranceCount(1);
        setOccurancesListInOrder(null);
        setOccuranceType(OccuranceType.ONE_FILLED_IN_ONE);
    }


    @Override
    public String toString() {
        return "Type: " + occuranceType + " ( partitionCount=" + partitionLength +
                ", randomOccuranceCount=" + randomOccuranceCount +
                ") OR ( occurancesListInOrder=" + occurancesListInOrder + " )";
    }

    // Test
    public static void main(String[] args) {
        List<String> testInputs = Arrays.asList("2", "3/5", "0,1,0,1,0,0", "abc", null, "4/xyz");

        OccurrenceParser occurrenceParser = new OccurrenceParser();

        for (String input : testInputs) {
            occurrenceParser.parse(input);
            System.out.println(occurrenceParser + " <- " + "Raw Input: " + input);
        }
    }




}
