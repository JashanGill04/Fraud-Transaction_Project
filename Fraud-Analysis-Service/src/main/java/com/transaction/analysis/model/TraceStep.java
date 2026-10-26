package com.transaction.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One stage of the fraud pipeline, recorded for the UI.
 * stage:  VELOCITY | GEOLOCATION | AI
 * status: PASS | FAIL | SKIPPED | ERROR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceStep {
    private String stage;
    private String title;
    private String status;
    private String summary;
    private Map<String, Object> details;
}