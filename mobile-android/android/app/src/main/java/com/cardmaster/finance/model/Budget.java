package com.cardmaster.finance.model;

import java.util.ArrayList;
import java.util.List;

public class Budget {
    private String id;
    private String name;
    private String description;
    private long updatedAt;

    private List<BudgetItem> items = new ArrayList<>();

    public Budget() {}

    public Budget(String id, String name, String description, long updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<BudgetItem> getItems() { return items; }
    public void setItems(List<BudgetItem> items) { this.items = items; }

    public double getTotalAmount() {
        double sum = 0;
        if (items != null) {
            for (BudgetItem item : items) sum += item.getAmount();
        }
        return sum;
    }
}

