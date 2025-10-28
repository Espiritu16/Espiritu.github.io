package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;

public class VentasKpiDto {

    private long ventasHoy;
    private BigDecimal ingresosMes;
    private long productosVendidosMes;
    private BigDecimal ticketPromedioMes;

    public long getVentasHoy() {
        return ventasHoy;
    }

    public void setVentasHoy(long ventasHoy) {
        this.ventasHoy = ventasHoy;
    }

    public BigDecimal getIngresosMes() {
        return ingresosMes;
    }

    public void setIngresosMes(BigDecimal ingresosMes) {
        this.ingresosMes = ingresosMes;
    }

    public long getProductosVendidosMes() {
        return productosVendidosMes;
    }

    public void setProductosVendidosMes(long productosVendidosMes) {
        this.productosVendidosMes = productosVendidosMes;
    }

    public BigDecimal getTicketPromedioMes() {
        return ticketPromedioMes;
    }

    public void setTicketPromedioMes(BigDecimal ticketPromedioMes) {
        this.ticketPromedioMes = ticketPromedioMes;
    }
}
