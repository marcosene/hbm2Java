package com.example.model;

import java.math.BigDecimal;
import java.util.Date;

public class Employee {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private Date hireDate;
    private Company company;
    private Department department;
    private Address address;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    
    public Date getHireDate() { return hireDate; }
    public void setHireDate(Date hireDate) { this.hireDate = hireDate; }
    
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}