// Resumen del inicio del tutor conectado, del periodo activo.
export interface EstudianteInicioTutor {
  id: number;
  nombre: string;
  carrera: string;
  nivelRiesgo: 'sin_riesgo' | 'bajo' | 'medio' | 'alto' | null;
}

export interface InicioTutor {
  estudiantes: number;
  sesionesRealizadas: number;
  reportesPendientes: number;
  sesionesProgramadas: number;
  estudiantesDestacados: EstudianteInicioTutor[];
}
