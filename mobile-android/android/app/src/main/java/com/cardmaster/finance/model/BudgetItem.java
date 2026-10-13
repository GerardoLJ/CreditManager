package com.cardmaster.finance.model;

public class BudgetItem {
    private String id;
    private String budgetId;
    private String concept;
    private double amount;
    private String type; // "fijo" o "variable"
    private String tag;
    private long updatedAt;

    public BudgetItem() {}

    public BudgetItem(String id, String budgetId, String concept, double amount, String type, String tag, long updatedAt) {
        this.id = id;
        this.budgetId = budgetId;
        this.concept = concept;
        this.amount = amount;
        this.type = type != null ? type : "fijo";
        this.tag = tag;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBudgetId() { return budgetId; }
    public void setBudgetId(String budgetId) { this.budgetId = budgetId; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}

