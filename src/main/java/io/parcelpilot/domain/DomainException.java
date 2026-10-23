package io.parcelpilot.domain;

/** A business-rule failure with a stable, machine-readable code. */
public final class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** How the failure should be reported to a client. */
    public enum Kind { INVALID, RULE, CONFLICT, NOT_FOUND }

    private final String code;
    private final Kind kind;

    public DomainException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public static DomainException invalid(String code, String message) {
        return new DomainException(Kind.INVALID, code, message);
    }

    public static DomainException rule(String code, String message) {
        return new DomainException(Kind.RULE, code, message);
    }

    public String code() {
        return code;
    }

    public Kind kind() {
        return kind;
    }
}
