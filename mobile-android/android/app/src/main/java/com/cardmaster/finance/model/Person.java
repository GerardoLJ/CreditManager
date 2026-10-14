package com.cardmaster.finance.model;

import java.util.ArrayList;
import java.util.List;

public class Person {
    private String id;
    private String name;
    private String parentId; // null si es persona/deudor principal, o ID de la persona padre si es subdeudor
    private long updatedAt;

    // Métricas calculadas
    private double totalSpent;
    private double totalDelivered;
    private double netOwed;
    private double spentForSubPerson; // Monto gastado asignado específicamente a este subdeudor
    private List<Person> subPeople = new ArrayList<>(); // Lista de subdeudores asociados

    public Person() {}

    public Person(String id, String name, long updatedAt) {
        this(id, name, null, updatedAt);
    }

    public Person(String id, String name, String parentId, long updatedAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public boolean isSubPerson() {
        return parentId != null && !parentId.trim().isEmpty();
    }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public double getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(double totalDelivered) { this.totalDelivered = totalDelivered; }

    public double getNetOwed() { return netOwed; }
    public void setNetOwed(double netOwed) { this.netOwed = netOwed; }

    public double getSpentForSubPerson() { return spentForSubPerson; }
    public void setSpentForSubPerson(double spentForSubPerson) { this.spentForSubPerson = spentForSubPerson; }

    public List<Person> getSubPeople() { return subPeople; }
    public void setSubPeople(List<Person> subPeople) { this.subPeople = subPeople != null ? subPeople : new ArrayList<>(); }
}
