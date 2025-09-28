package com.marcosdeDesarrollo.demo.Service;

import com.marcosdeDesarrollo.demo.DTO.RolRequestDto;
import com.marcosdeDesarrollo.demo.DTO.RolResponseDto;
import com.marcosdeDesarrollo.demo.Entity.Rol;
import com.marcosdeDesarrollo.demo.Repository.RolRepository;
import com.marcosdeDesarrollo.demo.Repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<RolResponseDto> listarTodos() {
        return rolRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<RolResponseDto> buscarPorId(Integer id) {
        return rolRepository.findById(id).map(this::mapToResponse);
    }

    public RolResponseDto crear(RolRequestDto request) {
        validarNombreUnico(request.getNombre(), null);

        Rol rol = new Rol();
        rol.setNombre(normalizarNombre(request.getNombre()));
        rol.setDescripcion(request.getDescripcion());

        Rol guardado = rolRepository.save(rol);
        return mapToResponse(guardado);
    }

    public Optional<RolResponseDto> actualizar(Integer id, RolRequestDto request) {
        return rolRepository.findById(id).map(rol -> {
            validarNombreUnico(request.getNombre(), id);
            rol.setNombre(normalizarNombre(request.getNombre()));
            rol.setDescripcion(request.getDescripcion());
            Rol actualizado = rolRepository.save(rol);
            return mapToResponse(actualizado);
        });
    }

    private RolResponseDto mapToResponse(Rol rol) {
        RolResponseDto dto = new RolResponseDto();
        dto.setId(rol.getId());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setFechaCreacion(rol.getFechaCreacion());
        dto.setUsuariosAsignados(usuarioRepository.countByRol_Id(rol.getId()));
        return dto;
    }

    private void validarNombreUnico(String nombre, Integer id) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String normalizado = normalizarNombre(nombre);
        boolean existe = (id == null)
                ? rolRepository.existsByNombreIgnoreCase(normalizado)
                : rolRepository.existsByNombreIgnoreCaseAndIdNot(normalizado, id);
        if (existe) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre especificado");
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre != null ? nombre.trim() : null;
    }
}
