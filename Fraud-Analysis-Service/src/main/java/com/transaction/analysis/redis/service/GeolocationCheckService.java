package com.transaction.analysis.redis.service;


import com.transaction.analysis.redis.model.LocationSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeolocationCheckService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_PLAUSIBLE_SPEED_KMH = 900.0; // ~commercial flight speed
    private static final Duration LOCATION_TTL = Duration.ofHours(24);

    /**
     * Returns true if the new transaction location implies impossible travel
     * from the card's last known location.
     */
    public boolean isImpossibleTravel(String cardNumberHash, double newLat, double newLon, Instant newTimestamp) {
        String key = "location:" + cardNumberHash;

        LocationSnapshot previous = (LocationSnapshot) redisTemplate.opsForValue().get(key);

        // Always store the latest location for next time, regardless of outcome
        LocationSnapshot current = LocationSnapshot.builder()
                .latitude(newLat)
                .longitude(newLon)
                .timestamp(newTimestamp)
                .build();
        redisTemplate.opsForValue().set(key, current, LOCATION_TTL);

        if (previous == null) {
            log.debug("No previous location for card hash={}, skipping geolocation check", cardNumberHash);
            return false; // nothing to compare against yet
        }

        double distanceKm = haversineDistance(
                previous.getLatitude(), previous.getLongitude(),
                newLat, newLon
        );

        double hoursElapsed = Duration.between(previous.getTimestamp(), newTimestamp).toSeconds() / 3600.0;

        if (hoursElapsed <= 0) {
            hoursElapsed = 1.0 / 3600.0; // avoid divide-by-zero for near-simultaneous transactions; treat as 1 second
        }

        double impliedSpeedKmh = distanceKm / hoursElapsed;

        log.debug("Geolocation check for card hash={}: distance={}km, elapsed={}h, impliedSpeed={}km/h",
                cardNumberHash, distanceKm, hoursElapsed, impliedSpeedKmh);

        return impliedSpeedKmh > MAX_PLAUSIBLE_SPEED_KMH;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}