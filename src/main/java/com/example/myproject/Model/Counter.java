package com.example.myproject.Model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@lombok.Data
@Document(collection = "counters")
public class Counter {
    @Id
    private String id;
    private long sequenceValue;
}
