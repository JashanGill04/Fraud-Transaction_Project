package com.transaction.analysis.redis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSnapshot implements Serializable {
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
}