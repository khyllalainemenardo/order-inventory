package edu.cit.menardo.shop;

/** Error body for 400 / 404 / 409: { status, error, message }. */
public record ApiError(int status, String error, String message) {
}
