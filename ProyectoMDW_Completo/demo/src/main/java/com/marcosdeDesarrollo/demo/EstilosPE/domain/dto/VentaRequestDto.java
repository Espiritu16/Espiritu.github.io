package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.MetodoPago;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.TipoComprobante;
import java.util.ArrayList;
import java.util.List;

public class VentaRequestDto {

    private Integer clienteId;
    private String clienteNombre;
    private TipoComprobante tipoComprobante;
    private MetodoPago metodoPago;
    private String dni;
    private String ruc;
    private String referencia;
    private List<VentaDetalleRequestDto> detalles = new ArrayList<>();

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public List<VentaDetalleRequestDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<VentaDetalleRequestDto> detalles) {
        this.detalles = detalles;
    }
}
