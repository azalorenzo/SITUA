package com.progweb.backend.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioAvisosRecibidos;
import com.progweb.backend.respuestas.RespuestaAvisoRecibido;
import com.progweb.backend.respuestas.RespuestaAvisosMarcados;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Avisos del tutor conectado. La identidad sale siempre del JWT y solo se leen o marcan sus propias filas.
@Service
@Transactional(readOnly = true)
public class ServicioAvisosRecibidos {
    private static final int MAXIMO_AVISOS = 200;

    private final RepositorioAvisosRecibidos avisos;
    private final ServicioAutenticacion autenticacion;
    private final Clock reloj;

    public ServicioAvisosRecibidos(RepositorioAvisosRecibidos avisos, ServicioAutenticacion autenticacion, Clock reloj) {
        this.avisos = avisos;
        this.autenticacion = autenticacion;
        this.reloj = reloj;
    }

    // Comprueba que la cuenta siga activa y tenga rol de tutor. Devuelve usuarios.id, no tutores.id.
    private long usuarioTutor(String identificador) {
        var usuario = autenticacion.consultarSesion(identificador);
        if (!usuario.roles().contains("tutor")) {
            throw new AccesoRechazado(403, "Esta operación requiere una cuenta de tutor.");
        }
        return usuario.id();
    }

    public List<RespuestaAvisoRecibido> consultar(String identificador) {
        return avisos.listar(usuarioTutor(identificador), MAXIMO_AVISOS);
    }

    public com.progweb.backend.respuestas.PaginaAvisosRecibidos consultarPagina(String identificador,com.progweb.backend.solicitudes.FiltrosAvisosRecibidos filtros){
        long usuario=usuarioTutor(identificador),total=avisos.contarFiltrados(usuario,filtros);int paginas=Math.toIntExact((total+filtros.tamano()-1)/filtros.tamano());
        int pagina=Math.min(filtros.pagina(),Math.max(1,paginas));return new com.progweb.backend.respuestas.PaginaAvisosRecibidos(avisos.listarPagina(usuario,filtros,pagina),pagina,filtros.tamano(),total,paginas,avisos.noLeidos(usuario),avisos.importantes(usuario));
    }

    @Transactional
    public RespuestaAvisoRecibido marcarLeido(String identificador, long avisoId) {
        long usuario = usuarioTutor(identificador);
        avisos.buscar(usuario, avisoId).orElseThrow(() -> new AccesoRechazado(404, "No se encontró ese aviso."));
        avisos.marcarLeido(usuario, avisoId, LocalDateTime.now(reloj));
        return avisos.buscar(usuario, avisoId).orElseThrow(() -> new AccesoRechazado(404, "No se encontró ese aviso."));
    }

    @Transactional
    public RespuestaAvisosMarcados marcarTodosLeidos(String identificador) {
        return new RespuestaAvisosMarcados(avisos.marcarTodosLeidos(usuarioTutor(identificador), LocalDateTime.now(reloj)));
    }
}
