package com.jay.LetsSplitIt.Entities;

import lombok.Data;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

import java.time.LocalDate;
import java.util.List;

@Data
public class Expense {

    @Id
    private ObjectId Id;

    private ObjectId groupId;

    private ObjectId paidBy;

    private double amount;

    private List<ObjectId> splitBetween;

    private String splitType;

    @CreatedDate
    private LocalDate createdDate;

}
