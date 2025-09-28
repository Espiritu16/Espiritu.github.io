package com.marcosdeDesarrollo.demo.Controller;

import com.marcosdeDesarrollo.demo.DTO.RolRequestDto;
import com.marcosdeDesarrollo.demo.DTO.RolResponseDto;
import com.marcosdeDesarrollo.demo.Service.RolService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public List<RolResponseDto> listar() {
        return rolService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDto> obtenerPorId(@PathVariable Integer id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody RolRequestDto request) {
        try {
            RolResponseDto nuevo = rolService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody RolRequestDto request) {
        try {
            return rolService.actualizar(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Roles no deben eliminarse físicamente
    @PutMapping("/{id}/inactivar")
    public ResponseEntity<Map<String, String>> inactivarNoDisponible(@PathVariable Integer id) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Los roles no pueden eliminarse ni desactivarse desde la aplicación.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }
}
