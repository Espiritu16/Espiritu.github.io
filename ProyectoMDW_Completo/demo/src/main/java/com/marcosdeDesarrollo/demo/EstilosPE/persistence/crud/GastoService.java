package com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoUpdateRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoReporteDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.GastosRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.TiposGastoRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.UsuarioRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Gastos;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Tipos_Gasto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoGasto;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GastoService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Logger log = LoggerFactory.getLogger(GastoService.class);

    private final GastosRepository gastosRepository;
    private final TiposGastoRepository tiposGastoRepository;
    private final UsuarioRepository usuarioRepository;

    public GastoService(GastosRepository gastosRepository,
            TiposGastoRepository tiposGastoRepository,
            UsuarioRepository usuarioRepository) {
        this.gastosRepository = gastosRepository;
        this.tiposGastoRepository = tiposGastoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<GastoResponseDto> listar(String estado, String tipo,
            LocalDate fechaInicio, LocalDate fechaFin, String terminoBusqueda) {
        return filtrarGastos(estado, tipo, fechaInicio, fechaFin, terminoBusqueda)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GastoReporteDto generarReporte(String estado, String tipo,
            LocalDate fechaInicio, LocalDate fechaFin, String terminoBusqueda) {
        List<Gastos> filtrados = filtrarGastos(estado, tipo, fechaInicio, fechaFin, terminoBusqueda);

        BigDecimal totalGeneral = filtrados.stream()
                .map(Gastos::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> totalPorTipo = filtrados.stream()
                .collect(Collectors.groupingBy(
                        gasto -> gasto.getTipoGasto() != null
                                ? gasto.getTipoGasto().getNombreTipo()
                                : "Sin tipo",
                        Collectors.mapping(Gastos::getMonto,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<String, BigDecimal> totalPorEstado = filtrados.stream()
                .collect(Collectors.groupingBy(
                        gasto -> gasto.getEstado() != null ? gasto.getEstado().name() : "SIN_ESTADO",
                        Collectors.mapping(Gastos::getMonto,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        GastoReporteDto dto = new GastoReporteDto();
        dto.setTotalGeneral(totalGeneral);
        dto.setTotalPorTipo(totalPorTipo);
        dto.setTotalPorEstado(totalPorEstado);
        dto.setCantidadRegistros(filtrados.size());
        dto.setGastos(filtrados.stream().map(this::mapToResponse).collect(Collectors.toList()));
        return dto;
    }

    public GastoResponseDto crear(GastoRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del gasto son obligatorios");
        }

        try {
            log.debug("Creando gasto - tipo: {}, fecha: {}, monto: {}",
                    request.getTipo(), request.getFecha(), request.getMonto());

            Usuario usuarioActual = obtenerUsuarioActual()
                    .orElseThrow(() -> new IllegalStateException("No se pudo identificar al usuario autenticado"));

            Tipos_Gasto tipoGasto = obtenerTipoGasto(request.getTipo());
            LocalDate fecha = parseFecha(request.getFecha());
            BigDecimal monto = validarMonto(request.getMonto());

            Gastos gasto = new Gastos();
            gasto.setUsuario(usuarioActual);
            gasto.setTipoGasto(tipoGasto);
            gasto.setDescripcion(normalizarDescripcion(request.getDescripcion(), tipoGasto));
            gasto.setFecha(fecha != null ? fecha : LocalDate.now());
            gasto.setMonto(monto);
            gasto.setEstado(EstadoGasto.Pendiente);

            Gastos guardado = gastosRepository.save(gasto);
            return mapToResponse(guardado);
        } catch (RuntimeException e) {
            log.error("Error al crear gasto", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public GastoResponseDto obtenerPorId(Integer id) {
        Gastos gasto = gastosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El gasto indicado no existe"));
        return mapToResponse(gasto);
    }

    public GastoResponseDto actualizar(Integer id, GastoUpdateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del gasto son obligatorios");
        }

        Gastos gasto = gastosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El gasto indicado no existe"));

        if (StringUtils.hasText(request.getDescripcion())) {
            gasto.setDescripcion(request.getDescripcion().trim());
        }

        if (StringUtils.hasText(request.getTipo())) {
            Tipos_Gasto tipoGasto = obtenerTipoGasto(request.getTipo());
            gasto.setTipoGasto(tipoGasto);
        }

        if (StringUtils.hasText(request.getFecha())) {
            LocalDate fecha = parseFecha(request.getFecha());
            if (fecha != null) {
                gasto.setFecha(fecha);
            }
        }

        if (request.getMonto() != null) {
            gasto.setMonto(validarMonto(request.getMonto()));
        }

        if (StringUtils.hasText(request.getEstado())) {
            gasto.setEstado(parseEstado(request.getEstado()));
        }

        Gastos actualizado = gastosRepository.save(gasto);
        return mapToResponse(actualizado);
    }

    public void eliminar(Integer id) {
        if (!gastosRepository.existsById(id)) {
            throw new IllegalArgumentException("El gasto indicado no existe");
        }
        gastosRepository.deleteById(id);
    }

    private Optional<Usuario> obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return usuarioRepository.findById(userDetails.getId());
        }

        String username = Objects.toString(authentication.getName(), null);
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(username);
    }

    private Tipos_Gasto obtenerTipoGasto(String nombreTipo) {
        if (!StringUtils.hasText(nombreTipo)) {
            throw new IllegalArgumentException("El tipo de gasto es obligatorio");
        }
        String limpio = nombreTipo.trim();
        return tiposGastoRepository.findByNombreTipoIgnoreCase(limpio)
                .orElseGet(() -> crearTipoGasto(limpio));
    }

    private LocalDate parseFecha(String fecha) {
        if (!StringUtils.hasText(fecha)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(fecha.trim(), ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha del gasto no tiene un formato válido (yyyy-MM-dd)");
        }
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto del gasto no puede ser negativo");
        }
        return monto;
    }

    private EstadoGasto parseEstado(String estado) {
        String limpio = estado.trim().toUpperCase(Locale.ROOT);
        return switch (limpio) {
            case "PENDIENTE" -> EstadoGasto.Pendiente;
            case "COMPLETADO" -> EstadoGasto.Completado;
            case "CANCELADO" -> EstadoGasto.Cancelado;
            default -> throw new IllegalArgumentException("Estado de gasto no válido: " + estado);
        };
    }

    private String normalizarDescripcion(String descripcion, Tipos_Gasto tipoGasto) {
        if (StringUtils.hasText(descripcion)) {
            return descripcion.trim();
        }
        return "Gasto - " + tipoGasto.getNombreTipo();
    }

    private GastoResponseDto mapToResponse(Gastos gasto) {
        GastoResponseDto dto = new GastoResponseDto();
        dto.setId(gasto.getIdGasto());
        dto.setDescripcion(gasto.getDescripcion());
        dto.setMonto(gasto.getMonto());
        dto.setTipo(gasto.getTipoGasto() != null ? gasto.getTipoGasto().getNombreTipo() : null);
        dto.setEstado(gasto.getEstado() != null ? gasto.getEstado().name() : null);
        dto.setFecha(gasto.getFecha() != null ? gasto.getFecha().format(ISO_DATE) : null);
        dto.setFechaCreacion(formatDateTime(gasto.getFechaCreacion()));

        if (gasto.getUsuario() != null) {
            dto.setRegistradoPor(gasto.getUsuario().getNombreUsuario());
            dto.setUsuarioId(gasto.getUsuario().getId());
        }

        return dto;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }

    private Tipos_Gasto crearTipoGasto(String nombreTipo) {
        if (!StringUtils.hasText(nombreTipo)) {
            throw new IllegalArgumentException("El tipo de gasto es obligatorio");
        }
        Tipos_Gasto nuevo = new Tipos_Gasto();
        nuevo.setNombreTipo(nombreTipo.trim());
        return tiposGastoRepository.save(nuevo);
    }

    private List<Gastos> filtrarGastos(String estado, String tipo,
            LocalDate fechaInicio, LocalDate fechaFin, String terminoBusqueda) {
        List<Gastos> gastos = gastosRepository.findAll(); // Obtener todos los gastos

        return gastos.stream()
                .filter(gasto -> {
                    boolean cumpleEstado = true;
                    if (StringUtils.hasText(estado)) {
                        cumpleEstado = gasto.getEstado() != null && gasto.getEstado().name().equalsIgnoreCase(estado.trim());
                    }

                    boolean cumpleTipo = true;
                    if (StringUtils.hasText(tipo)) {
                        cumpleTipo = gasto.getTipoGasto() != null && gasto.getTipoGasto().getNombreTipo().equalsIgnoreCase(tipo.trim());
                    }

                    boolean cumpleFecha = true;
                    if (fechaInicio != null && fechaFin != null) {
                        cumpleFecha = !gasto.getFecha().isBefore(fechaInicio) && !gasto.getFecha().isAfter(fechaFin);
                    } else if (fechaInicio != null) {
                        cumpleFecha = !gasto.getFecha().isBefore(fechaInicio);
                    } else if (fechaFin != null) {
                        cumpleFecha = !gasto.getFecha().isAfter(fechaFin);
                    }

                    boolean cumpleBusqueda = true;
                    if (StringUtils.hasText(terminoBusqueda)) {
                        String busquedaLower = terminoBusqueda.trim().toLowerCase();
                        cumpleBusqueda = (gasto.getDescripcion() != null && gasto.getDescripcion().toLowerCase().contains(busquedaLower))
                                || (gasto.getTipoGasto() != null && gasto.getTipoGasto().getNombreTipo().toLowerCase().contains(busquedaLower))
                                || (gasto.getUsuario() != null && gasto.getUsuario().getNombreUsuario().toLowerCase().contains(busquedaLower));
                    }

                    return cumpleEstado && cumpleTipo && cumpleFecha && cumpleBusqueda;
                })
                .collect(Collectors.toList());
    }
}
