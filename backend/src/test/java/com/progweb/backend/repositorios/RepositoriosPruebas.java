package com.progweb.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.progweb.backend.entidades.Rol;
import com.progweb.backend.entidades.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class RepositoriosPruebas {

    @Autowired
    private RepositorioUsuarios repositorioUsuarios;

    @Autowired
    private RepositorioRoles repositorioRoles;

    @PersistenceContext
    private EntityManager administradorEntidades;

    @Test
    void buscaUnUsuarioPorSuCorreoSinCargarLosRoles() {
        Usuario usuario = crearUsuario();
        usuario.asignarRol(repositorioRoles.buscarPorCodigo("tutor").orElseThrow());
        repositorioUsuarios.saveAndFlush(usuario);
        administradorEntidades.clear();

        Usuario encontrado = repositorioUsuarios.buscarPorCorreo("repositorios@example.invalid").orElseThrow();

        assertThat(encontrado.obtenerId()).isEqualTo(usuario.obtenerId());
        assertThat(encontrado.obtenerNombre()).isEqualTo("Prueba de repositorios");
        assertThat(administradorEntidades.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(encontrado, "roles")).isFalse();
    }

    @Test
    void consultaUnUsuarioConVariosRolesYPermiteLeerlosDespues() {
        Usuario usuario = crearUsuario();
        usuario.asignarRol(repositorioRoles.buscarPorCodigo("tutor").orElseThrow());
        usuario.asignarRol(repositorioRoles.buscarPorCodigo("estudiante").orElseThrow());
        repositorioUsuarios.saveAndFlush(usuario);
        administradorEntidades.clear();

        Usuario encontrado = repositorioUsuarios.buscarPorCorreoConRoles("repositorios@example.invalid")
                .orElseThrow();
        assertThat(administradorEntidades.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(encontrado, "roles")).isTrue();
        administradorEntidades.clear();

        assertThat(encontrado.obtenerRoles()).extracting(Rol::obtenerCodigo)
                .containsExactlyInAnyOrder("tutor", "estudiante");
    }

    @Test
    void tambienEncuentraUsuariosQueTodaviaNoTienenRoles() {
        repositorioUsuarios.saveAndFlush(crearUsuario());
        administradorEntidades.clear();

        Usuario encontrado = repositorioUsuarios.buscarPorCorreoConRoles("repositorios@example.invalid")
                .orElseThrow();
        administradorEntidades.clear();

        assertThat(encontrado.obtenerRoles()).isEmpty();
    }

    @Test
    void devuelveVacioCuandoNoExisteElUsuarioOElRol() {
        assertThat(repositorioUsuarios.buscarPorCorreo("ausente-repositorios@example.invalid")).isEmpty();
        assertThat(repositorioUsuarios.buscarPorCorreoConRoles("ausente-repositorios@example.invalid")).isEmpty();
        assertThat(repositorioRoles.buscarPorCodigo("rol_inexistente")).isEmpty();
    }

    @Test
    void buscaElRolPorCodigoYNoPorSuNombreVisible() {
        Rol encontrado = repositorioRoles.buscarPorCodigo("coordinador").orElseThrow();

        assertThat(encontrado.obtenerCodigo()).isEqualTo("coordinador");
        assertThat(encontrado.obtenerNombre()).isEqualTo("Coordinación");
        assertThat(repositorioRoles.buscarPorCodigo("Coordinación")).isEmpty();
    }

    @Test
    void trataLasEntradasComoDatosDeBusqueda() {
        String entrada = "' OR '1'='1";

        assertThat(repositorioUsuarios.buscarPorCorreo(entrada)).isEmpty();
        assertThat(repositorioUsuarios.buscarPorCorreoConRoles(entrada)).isEmpty();
        assertThat(repositorioRoles.buscarPorCodigo(entrada)).isEmpty();
    }

    private Usuario crearUsuario() {
        return new Usuario("Prueba de repositorios", "repositorios@example.invalid",
                "$2y$12$4oXUJVQ9Fs1DKmA1RNr3XOc3B4oa7kX0mTHkGv6oRjBGM8IQjP4Oe");
    }
}
