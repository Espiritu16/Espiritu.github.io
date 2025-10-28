package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentasKpiDto;
import java.time.LocalDate;
import java.util.List;

public interface VentaService {

    List<VentaResponseDto> listarVentas(String estado,
            String metodoPago,
            String tipoComprobante,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String search);

    VentaResponseDto obtenerVenta(Integer id);

    VentaResponseDto crearVenta(VentaRequestDto request);

    VentaResponseDto actualizarVenta(Integer id, VentaRequestDto request);

    void eliminarVenta(Integer id);

    VentasKpiDto obtenerKpis();

    List<VentaResponseDto> obtenerVentasDelMes();
}
