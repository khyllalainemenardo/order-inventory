package edu.cit.menardo.shop;

import java.util.List;

/** POST /api/orders body: { items: [{ productId, quantity }, ...] }. */
public record OrderRequest(List<Item> items) {

    public record Item(String productId, Integer quantity) {
    }
}
