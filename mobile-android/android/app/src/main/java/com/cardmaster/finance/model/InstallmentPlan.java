package com.cardmaster.finance.model;

public class InstallmentPlan {
    private String id;
    private String concept;
    private double totalAmount;
    private int months;
    private String startDate;
    private String cardId;
    private String personId;
    private int paidMonths;
    private long updatedAt;

    public InstallmentPlan() {}

    public InstallmentPlan(String id, String concept, double totalAmount, int months, String startDate, String cardId, String personId, int paidMonths, long updatedAt) {
        this.id = id;
        this.concept = concept;
        this.totalAmount = totalAmount;
        this.months = months;
        this.startDate = startDate;
        this.cardId = cardId;
        this.personId = personId;
        this.paidMonths = paidMonths;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public int getPaidMonths() { return paidMonths; }
    public void setPaidMonths(int paidMonths) { this.paidMonths = paidMonths; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getMonthlyAmount() {
        return months > 0 ? (totalAmount / months) : 0;
    }

    public double getPendingAmount() {
        int remaining = Math.max(0, months - paidMonths);
        return remaining * getMonthlyAmount();
    }
}

