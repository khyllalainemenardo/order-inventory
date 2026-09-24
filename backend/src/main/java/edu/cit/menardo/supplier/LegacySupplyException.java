package edu.cit.menardo.supplier;

import java.util.Set;

class LegacySupplyException extends RuntimeException {

    private static final Set<String> SESSION_ERRORS = Set.of("E-AUTH-02", "E-AUTH-03", "E-AUTH-07");
    private static final Set<Integer> BAD_ORDER_STATUSES = Set.of(400, 409, 415, 422);

    private final boolean sessionProblem;
    private final boolean retryable;
    private final boolean rejected;

    private LegacySupplyException(String message, boolean sessionProblem, boolean retryable, boolean rejected) {
        super(message);
        this.sessionProblem = sessionProblem;
        this.retryable = retryable;
        this.rejected = rejected;
    }

    static LegacySupplyException noResponse(String reason) {
        return new LegacySupplyException("No response: " + reason, false, true, false);
    }

    static LegacySupplyException fromResponse(int httpStatus, String code) {
        boolean sessionProblem = SESSION_ERRORS.contains(code);
        boolean serverProblem = httpStatus >= 500;
        boolean badOrder = BAD_ORDER_STATUSES.contains(httpStatus);
        return new LegacySupplyException(code + " (HTTP " + httpStatus + ")",
                sessionProblem, sessionProblem || serverProblem, badOrder);
    }

    boolean isSessionProblem() {
        return sessionProblem;
    }

    boolean isRetryable() {
        return retryable;
    }

    boolean isRejected() {
        return rejected;
    }
}
