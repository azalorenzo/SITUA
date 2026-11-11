package com.progweb.backend.entidades;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Column(length = 30)
    private String telefono;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(nullable = false, columnDefinition = "enum('activo','inactivo')")
    private String estado = "activo";

    // Dejamos que MySQL guarde las fechas, igual que en las tablas existentes.
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    // Los roles se consultan cuando hacen falta y se comparten entre usuarios.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new LinkedHashSet<>();

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, String contrasenaHash) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
    }

    public Long obtenerId() {
        return id;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    public String obtenerCorreo() {
        return correo;
    }

    public void cambiarCorreo(String correo) {
        this.correo = correo;
    }

    public String obtenerContrasenaHash() {
        return contrasenaHash;
    }

    public void cambiarContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String obtenerTelefono() {
        return telefono;
    }

    public void cambiarTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String obtenerAvatarUrl() {
        return avatarUrl;
    }

    public void cambiarAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String obtenerEstado() {
        return estado;
    }

    public void activar() {
        estado = "activo";
    }

    public void desactivar() {
        estado = "inactivo";
    }

    public LocalDateTime obtenerCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime obtenerActualizadoEn() {
        return actualizadoEn;
    }

    public Set<Rol> obtenerRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void asignarRol(Rol rol) {
        roles.add(rol);
    }

    public void quitarRol(Rol rol) {
        roles.remove(rol);
    }
}
