package edu.cit.menardo.shop;

import java.util.List;

public record OrderRequest(List<Item> items) {

    public record Item(String productId, Integer quantity) {
    }
}
