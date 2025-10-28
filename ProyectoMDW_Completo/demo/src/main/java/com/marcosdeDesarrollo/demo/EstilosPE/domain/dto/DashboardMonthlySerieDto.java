package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;

public class DashboardMonthlySerieDto {

    private String etiqueta;
    private BigDecimal total;

    public DashboardMonthlySerieDto() {
    }

    public DashboardMonthlySerieDto(String etiqueta, BigDecimal total) {
        this.etiqueta = etiqueta;
        this.total = total;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
