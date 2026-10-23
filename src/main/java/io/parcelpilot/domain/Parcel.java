package io.parcelpilot.domain;

import java.util.Arrays;

/**
 * A parcel's physical size. Couriers bill the higher of actual and volumetric weight
 * (cm³ ÷ 5000 = kg), rounded up to the next 500 g slab.
 */
public record Parcel(int weightGrams, int lengthCm, int widthCm, int heightCm) {

    public static final int MAX_WEIGHT_GRAMS = 50_000;
    public static final int MAX_SIDE_CM = 150;
    /** Longest side plus twice the other two. */
    public static final int MAX_LENGTH_PLUS_GIRTH_CM = 300;
    public static final int SLAB_GRAMS = 500;

    public Parcel {
        if (weightGrams < 1 || weightGrams > MAX_WEIGHT_GRAMS) {
            throw DomainException.invalid("WEIGHT_OUT_OF_RANGE", "Weight must be between 1 g and 50 kg.");
        }
        int[] sides = {lengthCm, widthCm, heightCm};
        Arrays.sort(sides);
        if (sides[0] < 1 || sides[2] > MAX_SIDE_CM) {
            throw DomainException.invalid("DIMENSION_OUT_OF_RANGE", "Each side must be between 1 and 150 cm.");
        }
        if (sides[2] + 2 * (sides[0] + sides[1]) > MAX_LENGTH_PLUS_GIRTH_CM) {
            throw DomainException.rule("OVERSIZE", "Longest side plus girth must not exceed 300 cm.");
        }
    }

    /** cm³ ÷ 5000 gives kilograms, so grams are cm³ ÷ 5, rounded up. */
    public int volumetricGrams() {
        long cubicCm = (long) lengthCm * widthCm * heightCm;
        return (int) ((cubicCm + 4) / 5);
    }

    public int chargeableGrams() {
        return slabs() * SLAB_GRAMS;
    }

    public int slabs() {
        int billed = Math.max(weightGrams, volumetricGrams());
        return (billed + SLAB_GRAMS - 1) / SLAB_GRAMS;
    }

    public boolean volumetricApplies() {
        return volumetricGrams() > weightGrams;
    }
}
