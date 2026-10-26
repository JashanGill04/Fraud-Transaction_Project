package com.transaction.analysis.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the stored JSON as-is, so the response shape is exactly what TraceStore saved.
 */
@RestController
@RequestMapping("/api/analysis/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceStore traceStore;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String list(@RequestParam(defaultValue = "50") int limit) {
        return traceStore.listJson(Math.min(limit, 200));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(@PathVariable String id) {
        return traceStore.getJson(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}