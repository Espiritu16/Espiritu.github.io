package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;

public class DashboardSummaryDto {

    private long ventasDia;
    private BigDecimal ingresosDia;
    private long ventasPendientes;
    private BigDecimal ingresosMes;
    private BigDecimal ticketPromedioMes;
    private long productosVendidosMes;

    public long getVentasDia() {
        return ventasDia;
    }

    public void setVentasDia(long ventasDia) {
        this.ventasDia = ventasDia;
    }

    public BigDecimal getIngresosDia() {
        return ingresosDia;
    }

    public void setIngresosDia(BigDecimal ingresosDia) {
        this.ingresosDia = ingresosDia;
    }

    public long getVentasPendientes() {
        return ventasPendientes;
    }

    public void setVentasPendientes(long ventasPendientes) {
        this.ventasPendientes = ventasPendientes;
    }

    public BigDecimal getIngresosMes() {
        return ingresosMes;
    }

    public void setIngresosMes(BigDecimal ingresosMes) {
        this.ingresosMes = ingresosMes;
    }

    public BigDecimal getTicketPromedioMes() {
        return ticketPromedioMes;
    }

    public void setTicketPromedioMes(BigDecimal ticketPromedioMes) {
        this.ticketPromedioMes = ticketPromedioMes;
    }

    public long getProductosVendidosMes() {
        return productosVendidosMes;
    }

    public void setProductosVendidosMes(long productosVendidosMes) {
        this.productosVendidosMes = productosVendidosMes;
    }
}
