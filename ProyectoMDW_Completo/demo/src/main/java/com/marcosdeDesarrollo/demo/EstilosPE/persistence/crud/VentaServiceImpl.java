package com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaDetalleRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaDetalleResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentasKpiDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Clientes;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.DetalleVenta;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.MetodoPago;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoVenta;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Producto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.TipoComprobante;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Venta;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.DetalleVentaQueryService;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.VentaQueryService;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.VentaService;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.ClientesRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.ProductoRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.UsuarioRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.VentaRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class VentaServiceImpl implements VentaService {

    private static final DateTimeFormatter REFERENCIA_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClientesRepository clientesRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaQueryService ventaQueryService;
    private final DetalleVentaQueryService detalleVentaQueryService;

    public VentaServiceImpl(VentaRepository ventaRepository,
            ProductoRepository productoRepository,
            ClientesRepository clientesRepository,
            UsuarioRepository usuarioRepository,
            VentaQueryService ventaQueryService,
            DetalleVentaQueryService detalleVentaQueryService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clientesRepository = clientesRepository;
        this.usuarioRepository = usuarioRepository;
        this.ventaQueryService = ventaQueryService;
        this.detalleVentaQueryService = detalleVentaQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDto> listarVentas(String estado,
            String metodoPago,
            String tipoComprobante,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String search) {

        Specification<Venta> specification = construirSpecification(estado, metodoPago, tipoComprobante, fechaInicio, fechaFin, search);

        List<Venta> ventas = ventaRepository.findAll(specification,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("idVenta")));

        return ventas.stream()
                .map(venta -> mapToResponse(venta, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDto obtenerVenta(Integer id) {
        Venta venta = ventaQueryService.obtenerVentaConDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("La venta indicada no existe"));
        return mapToResponse(venta, true);
    }

    @Override
    public VentaResponseDto crearVenta(VentaRequestDto request) {
        validarSolicitud(request);

        TipoComprobante tipoComprobante = request.getTipoComprobante() != null
                ? request.getTipoComprobante()
                : TipoComprobante.BoletaSimple;

        String referenciaNormalizada = normalizarCadena(request.getReferencia());
        String dniNormalizado = normalizarCadena(request.getDni());
        String rucNormalizado = normalizarCadena(request.getRuc());

        validarDocumentos(tipoComprobante, dniNormalizado, rucNormalizado);

        Usuario usuario = obtenerUsuarioActual()
                .orElseThrow(() -> new IllegalStateException("No se pudo resolver el usuario autenticado"));

        Clientes cliente = resolverCliente(request, dniNormalizado, rucNormalizado);

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setMetodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPago.Efectivo);
        venta.setTipoComprobante(tipoComprobante);

        switch (tipoComprobante) {
            case Boleta -> {
                venta.setDni(dniNormalizado);
                venta.setRuc(null);
            }
            case Factura -> {
                venta.setRuc(rucNormalizado);
                venta.setDni(null);
            }
            case BoletaSimple -> {
                venta.setDni(null);
                venta.setRuc(null);
            }
        }

        String referenciaAsignada = (referenciaNormalizada == null || referenciaNormalizada.isBlank())
                ? generarReferencia()
                : referenciaNormalizada;
        venta.setReferencia(referenciaAsignada);

        List<AjusteStock> ajustesAplicados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try {
            for (VentaDetalleRequestDto detalleDto : request.getDetalles()) {
                DetalleVenta detalle = construirDetalle(venta, detalleDto, ajustesAplicados);
                venta.getDetalles().add(detalle);
                total = total.add(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }
        } catch (RuntimeException ex) {
            revertirAjustes(ajustesAplicados);
            throw ex;
        }

        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        return mapToResponse(guardada, true);
    }

    @Override
    public VentaResponseDto actualizarVenta(Integer id, VentaRequestDto request) {
        validarSolicitud(request);

        Venta venta = ventaQueryService.obtenerVentaConDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("La venta indicada no existe"));

        TipoComprobante tipoComprobante = request.getTipoComprobante() != null
                ? request.getTipoComprobante()
                : TipoComprobante.BoletaSimple;

        String referenciaNormalizada = normalizarCadena(request.getReferencia());
        String dniNormalizado = normalizarCadena(request.getDni());
        String rucNormalizado = normalizarCadena(request.getRuc());

        validarDocumentos(tipoComprobante, dniNormalizado, rucNormalizado);

        restaurarStock(venta);
        venta.getDetalles().clear();

        venta.setMetodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPago.Efectivo);
        venta.setTipoComprobante(tipoComprobante);

        switch (tipoComprobante) {
            case Boleta -> {
                venta.setDni(dniNormalizado);
                venta.setRuc(null);
            }
            case Factura -> {
                venta.setRuc(rucNormalizado);
                venta.setDni(null);
            }
            case BoletaSimple -> {
                venta.setDni(null);
                venta.setRuc(null);
            }
        }

        String referenciaAsignada = venta.getReferencia() != null
                ? venta.getReferencia()
                : (referenciaNormalizada != null && !referenciaNormalizada.isBlank()
                        ? referenciaNormalizada
                        : generarReferencia());
        venta.setReferencia(referenciaAsignada);

        Clientes cliente = resolverCliente(request, dniNormalizado, rucNormalizado);
        venta.setCliente(cliente);

        List<AjusteStock> ajustesAplicados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try {
            for (VentaDetalleRequestDto detalleDto : request.getDetalles()) {
                DetalleVenta detalle = construirDetalle(venta, detalleDto, ajustesAplicados);
                venta.getDetalles().add(detalle);
                total = total.add(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }
        } catch (RuntimeException ex) {
            revertirAjustes(ajustesAplicados);
            throw ex;
        }

        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        return mapToResponse(guardada, true);
    }

    @Override
    public void eliminarVenta(Integer id) {
        Venta venta = ventaQueryService.obtenerVentaConDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("La venta indicada no existe"));

        restaurarStock(venta);
        ventaRepository.delete(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public VentasKpiDto obtenerKpis() {
        LocalDate hoy = LocalDate.now();
        long ventasHoy = ventaRepository.countByFecha(hoy);

        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        BigDecimal ingresosMes = ventaQueryService.calcularIngresosPorRango(inicioMes, finMes);
        long productosVendidos = detalleVentaQueryService.sumCantidadByFechaBetween(inicioMes, finMes);
        BigDecimal ticketPromedio = ventaQueryService.calcularTicketPromedio(inicioMes, finMes);

        VentasKpiDto dto = new VentasKpiDto();
        dto.setVentasHoy(ventasHoy);
        dto.setIngresosMes(ingresosMes);
        dto.setProductosVendidosMes(productosVendidos);
        dto.setTicketPromedioMes(ticketPromedio);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDto> obtenerVentasDelMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        return listarVentas(null, null, null, inicioMes, finMes, null);
    }

    private void validarSolicitud(VentaRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        if (request.getMetodoPago() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un producto en la venta");
        }
        if (request.getReferencia() != null && request.getReferencia().trim().length() > 100) {
            throw new IllegalArgumentException("La referencia no puede superar los 100 caracteres");
        }
    }

    private Clientes resolverCliente(VentaRequestDto request, String dni, String ruc) {
        if (request.getClienteId() != null) {
            Clientes existente = clientesRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("El cliente indicado no existe"));
            boolean actualizado = false;

            if (request.getClienteNombre() != null && !request.getClienteNombre().isBlank()) {
                String nombreCompleto = request.getClienteNombre().trim();
                String nombre = nombreCompleto;
                String apellido = null;

                int idx = nombreCompleto.indexOf(' ');
                if (idx > 0) {
                    nombre = nombreCompleto.substring(0, idx).trim();
                    apellido = nombreCompleto.substring(idx + 1).trim();
                }

                if (!Objects.equals(nombre, existente.getNombre())) {
                    existente.setNombre(nombre);
                    actualizado = true;
                }
                if (!Objects.equals(apellido, existente.getApellido())) {
                    existente.setApellido(apellido);
                    actualizado = true;
                }
            }
            if (dni != null && !dni.isBlank() && !dni.equals(existente.getDni())) {
                existente.setDni(dni);
                actualizado = true;
            }
            if (ruc != null && !ruc.isBlank() && !ruc.equals(existente.getRuc())) {
                existente.setRuc(ruc);
                actualizado = true;
            }
            return actualizado ? clientesRepository.save(existente) : existente;
        }

        if (request.getClienteNombre() == null || request.getClienteNombre().isBlank()) {
            return null;
        }

        String nombreCompleto = request.getClienteNombre().trim();
        String nombre = nombreCompleto;
        String apellido = null;

        int idx = nombreCompleto.indexOf(' ');
        if (idx > 0) {
            nombre = nombreCompleto.substring(0, idx).trim();
            apellido = nombreCompleto.substring(idx + 1).trim();
        }

        Clientes cliente = new Clientes();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setDni(dni);
        cliente.setRuc(ruc);
        cliente.setEstado(Estado.Activo);
        return clientesRepository.save(cliente);
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
        if (username == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(username);
    }

    private Specification<Venta> construirSpecification(String estado,
            String metodoPago,
            String tipoComprobante,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String search) {

        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(estado)) {
                predicates.add(cb.equal(root.get("estado"),
                        parseEnumIgnoreCase(estado, EstadoVenta.class, "El estado indicado no es válido")));
            }

            if (StringUtils.hasText(metodoPago)) {
                predicates.add(cb.equal(root.get("metodoPago"),
                        parseEnumIgnoreCase(metodoPago, MetodoPago.class, "El método de pago indicado no es válido")));
            }

            if (StringUtils.hasText(tipoComprobante)) {
                predicates.add(cb.equal(root.get("tipoComprobante"),
                        parseEnumIgnoreCase(tipoComprobante, TipoComprobante.class, "El tipo de comprobante indicado no es válido")));
            }

            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicio));
            }

            if (fechaFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaFin));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Venta, Clientes> clienteJoin = root.join("cliente", JoinType.LEFT);
                Join<Venta, Usuario> usuarioJoin = root.join("usuario", JoinType.LEFT);

                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(cb.like(cb.lower(root.get("referencia")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("dni")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("ruc")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("metodoPago").as(String.class)), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("tipoComprobante").as(String.class)), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("estado").as(String.class)), pattern));
                searchPredicates.add(cb.like(cb.lower(clienteJoin.get("nombre")), pattern));
                searchPredicates.add(cb.like(cb.lower(clienteJoin.get("apellido")), pattern));
                searchPredicates.add(cb.like(cb.lower(usuarioJoin.get("nombreUsuario")), pattern));

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private <E extends Enum<E>> E parseEnumIgnoreCase(String value, Class<E> enumType, String mensajeError) {
        return Arrays.stream(enumType.getEnumConstants())
                .filter(item -> item.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(mensajeError));
    }

    private VentaResponseDto mapToResponse(Venta venta, boolean incluirDetalles) {
        VentaResponseDto dto = new VentaResponseDto();
        dto.setId(venta.getIdVenta());
        dto.setFecha(venta.getFecha());
        if (venta.getCliente() != null) {
            dto.setClienteId(venta.getCliente().getIdCliente());
            String nombreCliente = venta.getCliente().getNombre();
            if (venta.getCliente().getApellido() != null && !venta.getCliente().getApellido().isBlank()) {
                nombreCliente = nombreCliente + " " + venta.getCliente().getApellido();
            }
            dto.setCliente(nombreCliente);
        } else {
            dto.setCliente("Venta mostrador");
        }

        if (venta.getUsuario() != null) {
            dto.setUsuarioId(venta.getUsuario().getId());
            dto.setUsuario(venta.getUsuario().getNombreUsuario());
        }

        dto.setMetodoPago(venta.getMetodoPago() != null ? venta.getMetodoPago().name() : null);
        dto.setEstado(venta.getEstado() != null ? venta.getEstado().name() : null);
        dto.setTotal(venta.getTotal());
        dto.setReferencia(venta.getReferencia());
        dto.setTipoComprobante(venta.getTipoComprobante() != null ? venta.getTipoComprobante().name() : null);
        dto.setDni(venta.getDni());
        dto.setRuc(venta.getRuc());

        if (incluirDetalles) {
            dto.setDetalles(venta.getDetalles().stream().map(detalle -> {
                VentaDetalleResponseDto detalleDto = new VentaDetalleResponseDto();
                detalleDto.setIdDetalle(detalle.getIdDetalle());
                if (detalle.getProducto() != null) {
                    detalleDto.setProductoId(detalle.getProducto().getIdProducto());
                    detalleDto.setSku(detalle.getProducto().getSku());
                    detalleDto.setNombreProducto(detalle.getProducto().getNombreProducto());
                }
                detalleDto.setCantidad(detalle.getCantidad());
                detalleDto.setPrecioUnitario(detalle.getPrecioUnitario());
                BigDecimal subtotal = detalle.getSubtotal();
                if (subtotal == null && detalle.getPrecioUnitario() != null && detalle.getCantidad() != null) {
                    subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
                }
                detalleDto.setSubtotal(subtotal);
                return detalleDto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }

    private String normalizarCadena(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validarDocumentos(TipoComprobante tipo, String dni, String ruc) {
        switch (tipo) {
            case Boleta -> {
                if (dni == null || !dni.matches("\\d{8}")) {
                    throw new IllegalArgumentException("Para boletas se requiere un DNI de 8 dígitos");
                }
            }
            case Factura -> {
                if (ruc == null || !ruc.matches("\\d{11}")) {
                    throw new IllegalArgumentException("Para facturas se requiere un RUC de 11 dígitos");
                }
            }
            case BoletaSimple -> {
                // No requiere documentos
            }
        }
    }

    private String generarReferencia() {
        return "VEN-" + LocalDateTime.now().format(REFERENCIA_FORMATTER);
    }

    private void restaurarStock(Venta venta) {
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();
            if (producto != null) {
                aplicarAjusteStock(producto, detalle.getCantidad(), null);
            }
        }
    }

    private DetalleVenta construirDetalle(Venta venta,
            VentaDetalleRequestDto detalleDto,
            List<AjusteStock> ajustesAplicados) {

        Producto producto = productoRepository.findById(detalleDto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + detalleDto.getProductoId()));

        Integer cantidad = detalleDto.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero para el producto " + producto.getNombreProducto());
        }

        BigDecimal precioUnitario = detalleDto.getPrecioUnitario() != null
                ? detalleDto.getPrecioUnitario()
                : producto.getPrecio();

        if (precioUnitario == null || BigDecimal.ZERO.compareTo(precioUnitario) >= 0) {
            throw new IllegalArgumentException("El precio unitario es obligatorio y debe ser mayor a cero");
        }

        aplicarAjusteStock(producto, -cantidad, ajustesAplicados);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precioUnitario);
        return detalle;
    }

    private void aplicarAjusteStock(Producto producto, int delta, List<AjusteStock> ajustesAplicados) {
        int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
        int nuevoStock = stockActual + delta;
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para el producto " + producto.getNombreProducto());
        }
        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);
        if (ajustesAplicados != null) {
            ajustesAplicados.add(new AjusteStock(producto, delta));
        }
    }

    private void revertirAjustes(List<AjusteStock> ajustesAplicados) {
        for (int i = ajustesAplicados.size() - 1; i >= 0; i--) {
            AjusteStock ajuste = ajustesAplicados.get(i);
            aplicarAjusteStock(ajuste.producto, -ajuste.delta, null);
        }
    }

    private static class AjusteStock {
        private final Producto producto;
        private final int delta;

        AjusteStock(Producto producto, int delta) {
            this.producto = producto;
            this.delta = delta;
        }
    }
}
