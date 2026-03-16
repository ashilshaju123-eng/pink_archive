package com.pinkarchive.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private final List<CartItem> items = new ArrayList<>();

    public List<CartItem> getItems() {
        return items;
    }

    public void addOrIncrement(String slug, String name, String size, int unitPricePence) {
        for (CartItem item : items) {
            if (item.getProductSlug().equals(slug) && item.getSize().equals(size)) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        items.add(new CartItem(slug, name, size, unitPricePence, 1));
    }

    public int getTotalPence() {
        return items.stream().mapToInt(CartItem::getLineTotalPence).sum();
    }

    public String getTotalFormatted() {
        return "£" + String.format("%.2f", getTotalPence() / 100.0);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}
