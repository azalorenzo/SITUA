package com.progweb.backend.respuestas;

import java.util.List;

public record RespuestaPerfilUsuario(RespuestaUsuario usuario,String rol,boolean puedeEditarCorreo,String telefono,String matricula,
        Long carreraId,Long periodoId,String periodo,Integer semestre,boolean puedeEditarAcademico,
        Long departamentoId,boolean aceptaSms,List<Opcion> carreras,List<Opcion> departamentos) {
    public record Opcion(long id,String nombre) {}
}
