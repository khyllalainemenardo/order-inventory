package edu.cit.menardo.shop;

import java.util.List;

/** The failure cases of the Order module, each mapped to an HTTP status in OrderErrorHandler. */
final class OrderExceptions {

    private OrderExceptions() {
    }

    /** 400 - the request itself is malformed (empty cart, bad quantity, unknown product). */
    static class InvalidOrderException extends RuntimeException {
        InvalidOrderException(String message) {
            super(message);
        }
    }

    /** 404 - no order with that id. */
    static class OrderNotFoundException extends RuntimeException {
        OrderNotFoundException(String orderId) {
            super("No order with id " + orderId + ".");
        }
    }

    /** 409 - the order exists but cannot be cancelled in its current state. */
    static class OrderStateException extends RuntimeException {
        OrderStateException(String message) {
            super(message);
        }
    }

    /**
     * Every line passed validation, but stock changed before one of them could be
     * reserved (another order got there first). Thrown so that @Transactional
     * rolls back the reservations already made - nothing is kept.
     */
    static class ReservationConflictException extends RuntimeException {

        private final List<OrderResponse.ItemOutcome> items;

        ReservationConflictException(String reason, List<OrderResponse.ItemOutcome> items) {
            super(reason);
            this.items = items;
        }

        List<OrderResponse.ItemOutcome> items() {
            return items;
        }
    }
}
