package com.cardmaster.finance.model;

public class SetAside {
    private String id;
    private String cardId;
    private String personId;
    private String movementId;
    private double amount;
    private String note;
    private String date;
    private String fundType; // "Efectivo" o "Débito"
    private int isPaid;
    private double paidAmount;
    private String status;
    private long updatedAt;

    // Campos auxiliares
    private String cardName;
    private String personName;

    public SetAside() {}

    public SetAside(String id, String cardId, String personId, String movementId, double amount, String note, String date, String fundType, int isPaid, double paidAmount, String status, long updatedAt) {
        this.id = id;
        this.cardId = cardId;
        this.personId = personId;
        this.movementId = movementId;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.fundType = fundType != null ? fundType : "Efectivo";
        this.isPaid = isPaid;
        this.paidAmount = paidAmount;
        this.status = status != null ? status : "apartado";
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public String getMovementId() { return movementId; }
    public void setMovementId(String movementId) { this.movementId = movementId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getFundType() { return fundType; }
    public void setFundType(String fundType) { this.fundType = fundType; }

    public int getIsPaid() { return isPaid; }
    public void setIsPaid(int isPaid) { this.isPaid = isPaid; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
}

