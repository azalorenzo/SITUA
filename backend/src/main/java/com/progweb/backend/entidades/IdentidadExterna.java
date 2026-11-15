package com.progweb.backend.entidades;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "identidades_externas")
public class IdentidadExterna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String proveedor = "google";

    @Column(name = "identificador_externo", nullable = false, length = 255)
    private String identificadorExterno;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected IdentidadExterna() { }

    public IdentidadExterna(Usuario usuario, String identificadorExterno) {
        this.usuario = usuario;
        this.identificadorExterno = identificadorExterno;
    }

    public Usuario obtenerUsuario() { return usuario; }
}
