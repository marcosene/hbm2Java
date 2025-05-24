package com.example.model;

import java.math.BigDecimal;
import java.util.Set;

public class Department {
    private Long id;
    private String name;
    private BigDecimal budget;
    private Company company;
    private Set<Employee> employees;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    
    public Set<Employee> getEmployees() { return employees; }
    public void setEmployees(Set<Employee> employees) { this.employees = employees; }
}