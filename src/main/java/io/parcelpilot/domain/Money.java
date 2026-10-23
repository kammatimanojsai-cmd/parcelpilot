package io.parcelpilot.domain;

/** Money is kept as whole paise (long) everywhere; these helpers round and format it. */
public final class Money {

    private Money() {}

    /** {@code amount × basisPoints ÷ 10000}, rounded half up. Amount must not be negative. */
    public static long percent(long amountPaise, int basisPoints) {
        if (amountPaise < 0) throw new IllegalArgumentException("amount must not be negative");
        return (amountPaise * basisPoints + 5_000) / 10_000;
    }

    /** Formats paise as rupees with Indian digit grouping, e.g. 12345678 → ₹1,23,456.78. */
    public static String format(long paise) {
        String sign = paise < 0 ? "-" : "";
        long abs = Math.abs(paise);
        String rupees = Long.toString(abs / 100);
        StringBuilder grouped = new StringBuilder();
        if (rupees.length() <= 3) {
            grouped.append(rupees);
        } else {
            String head = rupees.substring(0, rupees.length() - 3);
            String tail = rupees.substring(rupees.length() - 3);
            int first = head.length() % 2 == 0 ? 2 : 1;
            grouped.append(head, 0, first);
            for (int i = first; i < head.length(); i += 2) {
                grouped.append(',').append(head, i, i + 2);
            }
            grouped.append(',').append(tail);
        }
        return sign + "₹" + grouped + "." + String.format("%02d", abs % 100);
    }
}
