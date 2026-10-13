package com.cardmaster.finance.model;

public class Card {
    private String id;
    private String name;
    private double creditLimit;
    private int cutoffDay;
    private String color;
    private String logoBase64;
    private long updatedAt;

    private int displayOrder;

    // Métricas calculadas para la interfaz
    private double spentPeriod;
    private double spentMsi;
    private double totalOwed;
    private double available;
    private double utilizationPercent;
    private double totalSetAside;
    private double missingToSetAside;

    public Card() {}

    public Card(String id, String name, double creditLimit, int cutoffDay, String color, String logoBase64, long updatedAt) {
        this.id = id;
        this.name = name;
        this.creditLimit = creditLimit;
        this.cutoffDay = cutoffDay;
        this.color = color != null ? color : "#0284c7";
        this.logoBase64 = logoBase64;
        this.updatedAt = updatedAt;
        this.displayOrder = 0;
    }

    public Card(String id, String name, double creditLimit, int cutoffDay, String color, String logoBase64, long updatedAt, int displayOrder) {
        this.id = id;
        this.name = name;
        this.creditLimit = creditLimit;
        this.cutoffDay = cutoffDay;
        this.color = color != null ? color : "#0284c7";
        this.logoBase64 = logoBase64;
        this.updatedAt = updatedAt;
        this.displayOrder = displayOrder;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }

    public int getCutoffDay() { return cutoffDay; }
    public void setCutoffDay(int cutoffDay) { this.cutoffDay = cutoffDay; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLogoBase64() { return logoBase64; }
    public void setLogoBase64(String logoBase64) { this.logoBase64 = logoBase64; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public double getSpentPeriod() { return spentPeriod; }
    public void setSpentPeriod(double spentPeriod) { this.spentPeriod = spentPeriod; }

    public double getSpentMsi() { return spentMsi; }
    public void setSpentMsi(double spentMsi) { this.spentMsi = spentMsi; }

    public double getTotalOwed() { return totalOwed; }
    public void setTotalOwed(double totalOwed) { this.totalOwed = totalOwed; }

    public double getAvailable() { return available; }
    public void setAvailable(double available) { this.available = available; }

    private java.util.List<DebtorSummary> pendingDebtors = new java.util.ArrayList<>();

    public double getUtilizationPercent() { return utilizationPercent; }
    public void setUtilizationPercent(double utilizationPercent) { this.utilizationPercent = utilizationPercent; }

    public double getTotalSetAside() { return totalSetAside; }
    public void setTotalSetAside(double totalSetAside) { this.totalSetAside = totalSetAside; }

    public double getMissingToSetAside() { return missingToSetAside; }
    public void setMissingToSetAside(double missingToSetAside) { this.missingToSetAside = missingToSetAside; }

    public java.util.List<DebtorSummary> getPendingDebtors() { return pendingDebtors; }
    public void setPendingDebtors(java.util.List<DebtorSummary> pendingDebtors) { this.pendingDebtors = pendingDebtors; }

    public static class DebtorSummary {
        private String personId;
        private String personName;
        private double remainingAmount;

        public DebtorSummary(String personId, String personName, double remainingAmount) {
            this.personId = personId;
            this.personName = personName;
            this.remainingAmount = remainingAmount;
        }

        public String getPersonId() { return personId; }
        public String getPersonName() { return personName; }
        public double getRemainingAmount() { return remainingAmount; }
    }
}

