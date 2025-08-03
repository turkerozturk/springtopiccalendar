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

    public void parse(String inputExtended) {

        if (inputExtended == null || inputExtended.isBlank()) {
            getDefaultResult();
            return;
        }

        String temporaryString = inputExtended;
        boolean isDisabled = false;
        boolean isStrict = false;
        boolean isOpposite = false;
        String input = null;

        // 1. Başta ' varsa disable et ve baştaki ' karakterini sil
        if (temporaryString.startsWith("'")) {
            isDisabled = true;
            temporaryString = temporaryString.substring(1);
            getDefaultResult();
            return;
            // OccuranceType.DISABLED
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
                if(isStrict) {
                    setOccuranceType(OccuranceType.PATTERNED_BOTH_STRICT);
                } else{
                    if(isOpposite) {
                        setOccuranceType(OccuranceType.PATTERNED_EMPTY_LOOSE);
                    } else {
                        setOccuranceType(OccuranceType.PATTERNED_FILLED_LOOSE);
                    }
                }
            } else if (input.contains("/")) { // Format 2: a/b
                String[] parts = input.split("/");
                if (parts.length == 2) {
                    try {
                        setPartitionLength(Integer.parseInt(parts[1].trim()));
                        setRandomOccuranceCount(Integer.parseInt(parts[0].trim()));
                        setOccurancesListInOrder(null);
                        if(isStrict) {
                            if(isOpposite) {
                                setOccuranceType(OccuranceType.RANDOM_FILLED_STRICT);
                            } else {
                                setOccuranceType(OccuranceType.RANDOM_EMPTY_STRICT);
                            }
                        } else {
                            if(isOpposite) {
                                setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
                            } else {
                                setOccuranceType(OccuranceType.RANDOM_EMPTY_LOOSE);
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

                    if(isStrict) {
                        if(isOpposite) {
                            setOccuranceType(OccuranceType.RANDOM_FILLED_STRICT);
                        } else {
                            setOccuranceType(OccuranceType.RANDOM_EMPTY_STRICT);
                        }
                    } else {
                        if(isOpposite) {
                            setOccuranceType(OccuranceType.RANDOM_FILLED_LOOSE);
                        } else {
                            setOccuranceType(OccuranceType.RANDOM_EMPTY_LOOSE);
                        }
                    }
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
