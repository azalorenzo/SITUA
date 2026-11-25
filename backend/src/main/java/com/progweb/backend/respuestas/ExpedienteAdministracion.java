package com.progweb.backend.respuestas;
import java.math.BigDecimal;
import java.util.List;
public record ExpedienteAdministracion(long estudianteId,String nombre,String matricula,List<Periodo> periodos) {
 public record Periodo(long periodoId,String periodo,long carreraId,int semestre,String inscripcion,BigDecimal promedio,String riesgo,String condicion) { }
}
