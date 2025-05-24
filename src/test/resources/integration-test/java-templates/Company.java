package com.example.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Company {
    private Long id;
    private Integer modCount;
    private String name;
    private String description;
    private Date foundedDate;
    private BigDecimal revenue;
    private Boolean active;
    private String registrationNumber;
    private String taxId;
    private Address headquarters;
    private Set<Employee> employees;
    private List<Department> departments;
    private Set<Project> projects;
    private Map<String, BigDecimal> departmentBudgets;
    private Map<EmployeeKey, Integer> employeeRatings;
    private String detailedDescription;
    private Integer establishedYear;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getModCount() { return modCount; }
    public void setModCount(Integer modCount) { this.modCount = modCount; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getFoundedDate() { return foundedDate; }
    public void setFoundedDate(Date foundedDate) { this.foundedDate = foundedDate; }
    
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    
    public Address getHeadquarters() { return headquarters; }
    public void setHeadquarters(Address headquarters) { this.headquarters = headquarters; }
    
    public Set<Employee> getEmployees() { return employees; }
    public void setEmployees(Set<Employee> employees) { this.employees = employees; }
    
    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }
    
    public Set<Project> getProjects() { return projects; }
    public void setProjects(Set<Project> projects) { this.projects = projects; }
    
    public Map<String, BigDecimal> getDepartmentBudgets() { return departmentBudgets; }
    public void setDepartmentBudgets(Map<String, BigDecimal> departmentBudgets) { this.departmentBudgets = departmentBudgets; }
    
    public Map<EmployeeKey, Integer> getEmployeeRatings() { return employeeRatings; }
    public void setEmployeeRatings(Map<EmployeeKey, Integer> employeeRatings) { this.employeeRatings = employeeRatings; }
    
    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    
    public Integer getEstablishedYear() { return establishedYear; }
    public void setEstablishedYear(Integer establishedYear) { this.establishedYear = establishedYear; }
}