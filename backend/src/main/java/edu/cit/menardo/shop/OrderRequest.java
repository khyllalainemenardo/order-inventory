package edu.cit.menardo.shop;

/** POST /api/orders body. */
public record OrderRequest(String productId, Integer quantity) {
}
