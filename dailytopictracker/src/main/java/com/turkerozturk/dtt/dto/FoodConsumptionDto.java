package com.turkerozturk.dtt.dto;

import lombok.Data;

@Data
public class FoodConsumptionDto {

    private Double foodWeight;
    private Long topicId;
    private String topicName;
    private String categoryName;
    private Long categoryId;

    @Override
    public String toString() {
        return
                "" + foodWeight +

              //          " gr " + topicId +
             //   ", categoryId=" + categoryId +
             //   ", categoryName='" + categoryName + '\'' +
                " gr '" + topicName + '\''
              // + '}'
                ;
    }
}

    /*
    @Override
    public String toString() {
        return "FoodConsumptionDto{" +
                "topicId=" + topicId +
                ", foodWeight=" + foodWeight +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", topicName='" + topicName + '\'' +
                '}';
    }
*/

