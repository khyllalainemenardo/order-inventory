package edu.cit.menardo.shop;

import org.springframework.http.HttpStatus;

class OrderException extends RuntimeException {

    private final HttpStatus status;

    OrderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus getStatus() {
        return status;
    }
}
