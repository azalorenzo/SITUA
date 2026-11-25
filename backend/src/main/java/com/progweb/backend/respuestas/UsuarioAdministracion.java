package com.progweb.backend.respuestas;
import java.util.List;
public record UsuarioAdministracion(long id,String nombre,String correo,String estado,List<String> roles,String matricula,Long departamentoId,boolean vinculadoGoogle) { }
