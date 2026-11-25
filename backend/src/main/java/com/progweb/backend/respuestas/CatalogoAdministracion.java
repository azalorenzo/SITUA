package com.progweb.backend.respuestas;
import java.time.LocalDate;
public record CatalogoAdministracion(long id,String nombre,String clave,Long departamentoId,String departamento,String estado,LocalDate inicio,LocalDate fin) { }
