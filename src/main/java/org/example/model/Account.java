package org.example.model;

/**
 * 账户模型类
 */
public class Account {
    
    private Long id;
    private String name;
    private Double balance;
    
    public Account() {
    }
    
    public Account(String name, Double balance) {
        this.name = name;
        this.balance = balance;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Double getBalance() {
        return balance;
    }
    
    public void setBalance(Double balance) {
        this.balance = balance;
    }
    
    @Override
    public String toString() {
        return "Account{id=" + id + ", name='" + name + "', balance=" + balance + "}";
    }
}
