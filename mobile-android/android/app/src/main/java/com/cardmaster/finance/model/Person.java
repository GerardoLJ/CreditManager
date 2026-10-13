package com.cardmaster.finance.model;

public class Person {
    private String id;
    private String name;
    private long updatedAt;

    // Métricas calculadas
    private double totalSpent;
    private double totalDelivered;
    private double netOwed;

    public Person() {}

    public Person(String id, String name, long updatedAt) {
        this.id = id;
        this.name = name;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public double getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(double totalDelivered) { this.totalDelivered = totalDelivered; }

    public double getNetOwed() { return netOwed; }
    public void setNetOwed(double netOwed) { this.netOwed = netOwed; }
}

