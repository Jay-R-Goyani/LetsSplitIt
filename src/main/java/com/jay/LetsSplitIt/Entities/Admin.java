package com.jay.LetsSplitIt.Entities;

import lombok.Data;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;

@Data
public class Admin {
    @Id
    private ObjectId id;

    @NonNull
    private String name;

    @NonNull
    private String email;

    @NonNull
    private String password;

    @CreatedDate
    private LocalDate createdDate;

    @LastModifiedDate
    private LocalDate modifiedDate;
}
