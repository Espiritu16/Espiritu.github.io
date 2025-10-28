package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GastoQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> sumarMontoPorMes(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT YEAR(g.fecha) as anio, MONTH(g.fecha) as mes, COALESCE(SUM(g.monto), 0)
                FROM Gastos g
                WHERE g.fecha BETWEEN :inicio AND :fin
                GROUP BY YEAR(g.fecha), MONTH(g.fecha)
                ORDER BY anio, mes
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }

    public List<Object[]> sumarMontoPorDia(LocalDate inicio, LocalDate fin) {
        String jpql = """
                SELECT DAY(g.fecha) as dia, COALESCE(SUM(g.monto), 0)
                FROM Gastos g
                WHERE g.fecha BETWEEN :inicio AND :fin
                GROUP BY DAY(g.fecha)
                ORDER BY dia
                """;
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }
}
