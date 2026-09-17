package edu.cit.menardo.shop;

public record ApiError(int status, String error, String message) {
}
