package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardCategoriaDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardMonthlySerieDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardPaymentMethodDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardSummaryDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardVentaRecienteDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoVenta;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.MetodoPago;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Venta;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final VentaRepository ventaRepository;
    private final VentaQueryService ventaQueryService;
    private final GastoQueryService gastoQueryService;
    private final DetalleVentaQueryService detalleVentaQueryService;

    public DashboardService(VentaRepository ventaRepository,
            VentaQueryService ventaQueryService,
            GastoQueryService gastoQueryService,
            DetalleVentaQueryService detalleVentaQueryService) {
        this.ventaRepository = ventaRepository;
        this.ventaQueryService = ventaQueryService;
        this.gastoQueryService = gastoQueryService;
        this.detalleVentaQueryService = detalleVentaQueryService;
    }

    public DashboardSummaryDto obtenerResumen() {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioMes = mesActual.atDay(1);
        LocalDate finMes = mesActual.atEndOfMonth();

        long ventasHoy = ventaRepository.countByFecha(hoy);
        BigDecimal ingresosDia = asBigDecimal(ventaQueryService.sumarTotalPorFecha(hoy));
        BigDecimal ingresosMes = asBigDecimal(ventaQueryService.calcularIngresosPorRango(inicioMes, finMes));
        BigDecimal ticketPromedioMes = asBigDecimal(ventaQueryService.calcularTicketPromedio(inicioMes, finMes));
        long productosVendidosMes = detalleVentaQueryService.sumCantidadByFechaBetween(inicioMes, finMes);
        long pendientes = ventaRepository.countByEstado(EstadoVenta.Pendiente);

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setVentasDia(ventasHoy);
        dto.setIngresosDia(ingresosDia);
        dto.setIngresosMes(ingresosMes);
        dto.setTicketPromedioMes(ticketPromedioMes);
        dto.setProductosVendidosMes(productosVendidosMes);
        dto.setVentasPendientes(pendientes);
        return dto;
    }

    public List<DashboardVentaRecienteDto> obtenerVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        return ventaRepository.findByFechaOrderByFechaCreacionDesc(hoy)
                .stream()
                .limit(8)
                .map(this::mapToVentaReciente)
                .collect(Collectors.toList());
    }

    public List<DashboardMonthlySerieDto> obtenerVentasPorDia(int year, int month) {
        YearMonth periodo = YearMonth.of(year, month);
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        List<Object[]> registros = ventaQueryService.sumarTotalPorDia(inicio, fin);
        return construirSerieDiaria(registros, periodo);
    }

    public List<DashboardMonthlySerieDto> obtenerGastosPorDia(int year, int month) {
        YearMonth periodo = YearMonth.of(year, month);
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        List<Object[]> registros = gastoQueryService.sumarMontoPorDia(inicio, fin);
        return construirSerieDiaria(registros, periodo);
    }

    public List<DashboardMonthlySerieDto> obtenerVentasMensuales(int meses) {
        YearMonth actual = YearMonth.now();
        YearMonth inicioPeriodo = actual.minusMonths(meses - 1);
        LocalDate inicio = inicioPeriodo.atDay(1);
        LocalDate fin = actual.atEndOfMonth();

        Map<YearMonth, BigDecimal> datos = obtenerSerieMensual(ventaQueryService.sumarTotalPorMes(inicio, fin));
        return construirSerieOrdenada(datos, inicioPeriodo, actual);
    }

    public List<DashboardMonthlySerieDto> obtenerGastosMensuales(int meses) {
        YearMonth actual = YearMonth.now();
        YearMonth inicioPeriodo = actual.minusMonths(meses - 1);
        LocalDate inicio = inicioPeriodo.atDay(1);
        LocalDate fin = actual.atEndOfMonth();

        Map<YearMonth, BigDecimal> datos = obtenerSerieMensual(gastoQueryService.sumarMontoPorMes(inicio, fin));
        return construirSerieOrdenada(datos, inicioPeriodo, actual);
    }

    public List<DashboardCategoriaDto> obtenerTopCategorias(int limite) {
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.minusDays(29);
        List<Object[]> resultados = detalleVentaQueryService.topCategoriasPorPeriodo(inicio, fin);
        return resultados.stream()
                .limit(limite)
                .map(row -> new DashboardCategoriaDto(
                        Objects.toString(row[0], "Sin categoría"),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<DashboardPaymentMethodDto> obtenerMetodosPago(int meses) {
        YearMonth actual = YearMonth.now();
        YearMonth inicioPeriodo = actual.minusMonths(meses - 1);
        LocalDate inicio = inicioPeriodo.atDay(1);
        LocalDate fin = actual.atEndOfMonth();
        List<Object[]> resultados = ventaQueryService.contarPorMetodoPago(inicio, fin);
        return resultados.stream()
                .map(row -> new DashboardPaymentMethodDto(
                        formatearMetodoPago((MetodoPago) row[0]),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private Map<YearMonth, BigDecimal> obtenerSerieMensual(List<Object[]> registros) {
        Map<YearMonth, BigDecimal> datos = new HashMap<>();
        for (Object[] registro : registros) {
            int anio = ((Number) registro[0]).intValue();
            int mes = ((Number) registro[1]).intValue();
            BigDecimal total = asBigDecimal(registro[2]);
            datos.put(YearMonth.of(anio, mes), total);
        }
        return datos;
    }

    private List<DashboardMonthlySerieDto> construirSerieOrdenada(Map<YearMonth, BigDecimal> datos,
            YearMonth inicio, YearMonth fin) {
        List<DashboardMonthlySerieDto> serie = new ArrayList<>();
        YearMonth cursor = inicio;
        while (!cursor.isAfter(fin)) {
            BigDecimal total = datos.getOrDefault(cursor, BigDecimal.ZERO);
            String etiqueta = cursor.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_ES);
            etiqueta = etiqueta.substring(0, 1).toUpperCase(LOCALE_ES) + etiqueta.substring(1);
            etiqueta = etiqueta + " " + cursor.getYear();
            serie.add(new DashboardMonthlySerieDto(etiqueta, total));
            cursor = cursor.plusMonths(1);
        }
        return serie;
    }

    private List<DashboardMonthlySerieDto> construirSerieDiaria(List<Object[]> registros, YearMonth periodo) {
        Map<Integer, BigDecimal> datos = new HashMap<>();
        for (Object[] registro : registros) {
            int dia = ((Number) registro[0]).intValue();
            BigDecimal total = asBigDecimal(registro[1]);
            datos.put(dia, total);
        }
        List<DashboardMonthlySerieDto> serie = new ArrayList<>();
        int diasMes = periodo.lengthOfMonth();
        for (int dia = 1; dia <= diasMes; dia++) {
            BigDecimal total = datos.getOrDefault(dia, BigDecimal.ZERO);
            serie.add(new DashboardMonthlySerieDto(String.valueOf(dia), total));
        }
        return serie;
    }

    private DashboardVentaRecienteDto mapToVentaReciente(Venta venta) {
        DashboardVentaRecienteDto dto = new DashboardVentaRecienteDto();
        dto.setId(venta.getIdVenta());
        if (venta.getFechaCreacion() != null) {
            dto.setHora(String.format("%02d:%02d",
                    venta.getFechaCreacion().getHour(),
                    venta.getFechaCreacion().getMinute()));
        } else {
            dto.setHora("--:--");
        }
        dto.setCliente(venta.getCliente() != null
                ? (venta.getCliente().getNombre() != null ? venta.getCliente().getNombre() : "Cliente")
                : "Mostrador");
        dto.setVendedor(venta.getUsuario() != null ? venta.getUsuario().getNombreUsuario() : "—");
        dto.setMetodoPago(formatearMetodoPago(venta.getMetodoPago()));
        dto.setEstado(venta.getEstado() != null ? venta.getEstado().name() : "DESCONOCIDO");
        dto.setTotal(venta.getTotal());
        return dto;
    }

    private String formatearMetodoPago(MetodoPago metodoPago) {
        if (metodoPago == null) {
            return "Sin definir";
        }
        String texto = metodoPago.name().replace('_', ' ').toLowerCase(LOCALE_ES);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private BigDecimal asBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
