package com.progweb.backend.entidades;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class UsuarioRolPruebas {

    @PersistenceContext
    private EntityManager administradorEntidades;

    @Test
    void leeLosRolesQueYaExistenEnMySql() {
        List<Rol> roles = administradorEntidades.createQuery(
                "select rol from Rol rol order by rol.codigo", Rol.class).getResultList();

        assertThat(roles).extracting(Rol::obtenerCodigo)
                .containsExactly("coordinador", "estudiante", "tutor");
        assertThat(roles).allSatisfy(rol -> {
            assertThat(rol.obtenerNombre()).isNotBlank();
            assertThat(rol.obtenerCreadoEn()).isNotNull();
            assertThat(rol.obtenerActualizadoEn()).isNotNull();
        });
    }

    @Test
    void guardaYLeeUnUsuarioConVariosRoles() {
        Rol tutor = buscarRol("tutor");
        Rol estudiante = buscarRol("estudiante");
        Usuario usuario = crearUsuario();
        usuario.cambiarTelefono("5550001234");
        usuario.cambiarAvatarUrl("/imagenes/avatar-prueba.png");
        usuario.desactivar();
        usuario.asignarRol(tutor);
        usuario.asignarRol(estudiante);
        usuario.asignarRol(tutor);
        administradorEntidades.persist(usuario);
        administradorEntidades.flush();
        Long identificador = usuario.obtenerId();
        administradorEntidades.clear();

        Usuario guardado = administradorEntidades.find(Usuario.class, identificador);
        assertThat(administradorEntidades.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(guardado, "roles")).isFalse();
        assertThat(guardado.obtenerNombre()).isEqualTo("Usuario de prueba JPA");
        assertThat(guardado.obtenerCorreo()).isEqualTo("usuario-jpa@example.invalid");
        assertThat(guardado.obtenerContrasenaHash()).isEqualTo(contrasenaDePrueba());
        assertThat(guardado.obtenerTelefono()).isEqualTo("5550001234");
        assertThat(guardado.obtenerAvatarUrl()).isEqualTo("/imagenes/avatar-prueba.png");
        assertThat(guardado.obtenerEstado()).isEqualTo("inactivo");
        assertThat(guardado.obtenerCreadoEn()).isNotNull();
        assertThat(guardado.obtenerActualizadoEn()).isNotNull();
        assertThat(guardado.obtenerRoles()).extracting(Rol::obtenerCodigo)
                .containsExactlyInAnyOrder("tutor", "estudiante");
        Object fechaAsignacion = administradorEntidades.createNativeQuery(
                "select asignado_en from usuario_roles where usuario_id = :usuario limit 1", Object.class)
                .setParameter("usuario", identificador).getSingleResult();
        assertThat(fechaAsignacion).isNotNull();
    }

    @Test
    void quitarRolesYEliminarUnUsuarioNoEliminaLosRolesCompartidos() {
        Rol tutor = buscarRol("tutor");
        Rol estudiante = buscarRol("estudiante");
        Usuario usuario = crearUsuario();
        usuario.asignarRol(tutor);
        usuario.asignarRol(estudiante);
        administradorEntidades.persist(usuario);
        administradorEntidades.flush();

        usuario.quitarRol(tutor);
        administradorEntidades.flush();
        Long cantidadVinculos = administradorEntidades.createQuery(
                "select count(rol) from Usuario usuario join usuario.roles rol where usuario.id = :identificador",
                Long.class).setParameter("identificador", usuario.obtenerId()).getSingleResult();
        assertThat(cantidadVinculos).isEqualTo(1);
        assertThat(administradorEntidades.find(Rol.class, tutor.obtenerId())).isNotNull();

        administradorEntidades.remove(usuario);
        administradorEntidades.flush();
        assertThat(administradorEntidades.createQuery("select count(rol) from Rol rol", Long.class)
                .getSingleResult()).isEqualTo(3);
    }

    private Rol buscarRol(String codigo) {
        return administradorEntidades.createQuery("select rol from Rol rol where rol.codigo = :codigo", Rol.class)
                .setParameter("codigo", codigo).getSingleResult();
    }

    private Usuario crearUsuario() {
        return new Usuario("Usuario de prueba JPA", "usuario-jpa@example.invalid", contrasenaDePrueba());
    }

    private String contrasenaDePrueba() {
        // Solo usamos un hash de ejemplo; estas pruebas no comprueban el inicio de sesión.
        return "$2y$12$4oXUJVQ9Fs1DKmA1RNr3XOc3B4oa7kX0mTHkGv6oRjBGM8IQjP4Oe";
    }
}
