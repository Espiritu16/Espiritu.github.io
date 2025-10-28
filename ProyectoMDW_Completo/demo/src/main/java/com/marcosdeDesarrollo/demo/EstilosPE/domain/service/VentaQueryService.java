package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Venta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VentaQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    public BigDecimal calcularIngresosPorRango(LocalDate inicio, LocalDate fin) {
        String jpql = "SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin";
        BigDecimal resultado = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();
        return resultado != null ? resultado : BigDecimal.ZERO;
    }

    public BigDecimal calcularTicketPromedio(LocalDate inicio, LocalDate fin) {
        String jpql = "SELECT AVG(v.total) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin";
        Number resultado = entityManager.createQuery(jpql, Number.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();
        return resultado != null ? BigDecimal.valueOf(resultado.doubleValue()) : BigDecimal.ZERO;
    }

    public Optional<Venta> obtenerVentaConDetalles(Integer id) {
        String jpql = """
                SELECT v FROM Venta v
                LEFT JOIN FETCH v.detalles d
                LEFT JOIN FETCH d.producto
                WHERE v.idVenta = :id
                """;
        List<Venta> resultados = entityManager.createQuery(jpql, Venta.class)
                .setParameter("id", id)
                .getResultList();
        return resultados.stream().findFirst();
    }

    public BigDecimal sumarTotalPorFecha(LocalDate fecha) {
        String jpql = "SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha = :fecha";
        BigDecimal resultado = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("fecha", fecha)
                .getSingleResult();
        return resultado != null ? resultado : BigDecimal.ZERO;
    }

    public List<Object[]> sumarTotalPorMes(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT YEAR(v.fecha) as anio, MONTH(v.fecha) as mes, COALESCE(SUM(v.total), 0)
                FROM Venta v
                WHERE v.fecha BETWEEN :inicio AND :fin
                GROUP BY YEAR(v.fecha), MONTH(v.fecha)
                ORDER BY anio, mes
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }

    public List<Object[]> contarPorMetodoPago(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT v.metodoPago, COUNT(v)
                FROM Venta v
                WHERE v.fecha BETWEEN :inicio AND :fin
                GROUP BY v.metodoPago
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }

    public List<Object[]> sumarTotalPorDia(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT DAY(v.fecha) as dia, COALESCE(SUM(v.total), 0)
                FROM Venta v
                WHERE v.fecha BETWEEN :inicio AND :fin
                GROUP BY DAY(v.fecha)
                ORDER BY dia
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }
}
