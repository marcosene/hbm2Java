package com.example.model;

public class EmployeeKey {
    private Long employeeId;
    private Integer year;
    
    public EmployeeKey() {}
    
    public EmployeeKey(Long employeeId, Integer year) {
        this.employeeId = employeeId;
        this.year = year;
    }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeKey that = (EmployeeKey) o;
        return employeeId.equals(that.employeeId) && year.equals(that.year);
    }
    
    @Override
    public int hashCode() {
        return employeeId.hashCode() * 31 + year.hashCode();
    }
}