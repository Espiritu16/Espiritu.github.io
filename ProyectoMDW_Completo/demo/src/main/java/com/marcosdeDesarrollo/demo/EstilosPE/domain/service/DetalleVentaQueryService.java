package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DetalleVentaQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    public long sumCantidadByFechaBetween(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT COALESCE(SUM(d.cantidad), 0)
                FROM DetalleVenta d
                WHERE d.venta.fecha BETWEEN :inicio AND :fin
                """;
        Long resultado = entityManager.createQuery(jpql, Long.class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult();
        return resultado != null ? resultado : 0L;
    }

    public List<Object[]> topCategoriasPorPeriodo(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT d.producto.categoria.nombreCategoria, COALESCE(SUM(d.cantidad), 0)
                FROM DetalleVenta d
                WHERE d.venta.fecha BETWEEN :inicio AND :fin
                GROUP BY d.producto.categoria.nombreCategoria
                ORDER BY SUM(d.cantidad) DESC
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }
}
