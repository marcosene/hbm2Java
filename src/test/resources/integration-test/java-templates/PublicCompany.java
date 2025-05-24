package com.example.model;

import java.math.BigDecimal;

public class PublicCompany extends Company {
    private String stockSymbol;
    private BigDecimal marketCap;
    
    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
}