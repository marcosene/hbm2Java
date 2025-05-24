package com.example.model;

import java.util.Date;
import java.util.Set;

public class Project {
    private Long id;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private Set<Company> companies;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    
    public Set<Company> getCompanies() { return companies; }
    public void setCompanies(Set<Company> companies) { this.companies = companies; }
}