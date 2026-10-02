package edu.cit.menardo.channel;

import java.util.Set;

class TianggeException extends RuntimeException {

    private static final Set<Integer> FINAL_STATUSES = Set.of(400, 404, 409, 422);

    private final String code;
    private final boolean retryable;

    private TianggeException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    static TianggeException noResponse(String reason) {
        return new TianggeException("no_response", "No response: " + reason, true);
    }

    static TianggeException fromResponse(int httpStatus, String code, String message) {
        return new TianggeException(code, code + " (HTTP " + httpStatus + "): " + message,
                !FINAL_STATUSES.contains(httpStatus));
    }

    String code() {
        return code;
    }

    boolean isRetryable() {
        return retryable;
    }
}
