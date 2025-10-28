package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GastoReporteDto {

    private BigDecimal totalGeneral;
    private Map<String, BigDecimal> totalPorTipo;
    private Map<String, BigDecimal> totalPorEstado;
    private long cantidadRegistros;
    private List<GastoResponseDto> gastos;

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public Map<String, BigDecimal> getTotalPorTipo() {
        return totalPorTipo;
    }

    public void setTotalPorTipo(Map<String, BigDecimal> totalPorTipo) {
        this.totalPorTipo = totalPorTipo;
    }

    public Map<String, BigDecimal> getTotalPorEstado() {
        return totalPorEstado;
    }

    public void setTotalPorEstado(Map<String, BigDecimal> totalPorEstado) {
        this.totalPorEstado = totalPorEstado;
    }

    public long getCantidadRegistros() {
        return cantidadRegistros;
    }

    public void setCantidadRegistros(long cantidadRegistros) {
        this.cantidadRegistros = cantidadRegistros;
    }

    public List<GastoResponseDto> getGastos() {
        return gastos;
    }

    public void setGastos(List<GastoResponseDto> gastos) {
        this.gastos = gastos;
    }
}
