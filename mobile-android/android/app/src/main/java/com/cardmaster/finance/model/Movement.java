package com.cardmaster.finance.model;

public class Movement {
    private String id;
    private String concept;
    private double amount;
    private String date;
    private String cardId;
    private String personId;
    private String subPersonId; // ID del subdeudor / subetiqueta (opcional)
    private int isSetAside;
    private long updatedAt;

    // Campos auxiliares para visualización
    private String cardName;
    private String personName;
    private String subPersonName;
    private double totalSetAside;

    public Movement() {}

    public Movement(String id, String concept, double amount, String date, String cardId, String personId, int isSetAside, long updatedAt) {
        this(id, concept, amount, date, cardId, personId, null, isSetAside, updatedAt);
    }

    public Movement(String id, String concept, double amount, String date, String cardId, String personId, String subPersonId, int isSetAside, long updatedAt) {
        this.id = id;
        this.concept = concept;
        this.amount = amount;
        this.date = date;
        this.cardId = cardId;
        this.personId = personId;
        this.subPersonId = subPersonId;
        this.isSetAside = isSetAside;
        this.updatedAt = updatedAt;
        this.totalSetAside = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public String getSubPersonId() { return subPersonId; }
    public void setSubPersonId(String subPersonId) { this.subPersonId = subPersonId; }

    public int getIsSetAside() { return isSetAside; }
    public void setIsSetAside(int isSetAside) { this.isSetAside = isSetAside; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getSubPersonName() { return subPersonName; }
    public void setSubPersonName(String subPersonName) { this.subPersonName = subPersonName; }

    public double getTotalSetAside() { return totalSetAside; }
    public void setTotalSetAside(double totalSetAside) { this.totalSetAside = totalSetAside; }

    public double getPendingAmount() {
        return Math.max(0, amount - totalSetAside);
    }
}
