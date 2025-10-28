package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

// Esta será la estructura del JSON de respuesta exitosa.
public record JwtResponse(String token, Integer id, String email, String rol) {
}