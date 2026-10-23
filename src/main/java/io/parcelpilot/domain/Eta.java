package io.parcelpilot.domain;

import java.time.LocalDate;

/** Pickup date, delivery date and the working days in between. */
public record Eta(LocalDate pickupDate, LocalDate deliveryDate, int transitDays) {}
