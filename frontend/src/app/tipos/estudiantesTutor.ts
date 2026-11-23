// Estudiante asignado al tutor conectado. Las fechas llegan en UTC sin zona.
export interface EstudianteTutor {
  id: number;
  nombre: string;
  correo: string;
  telefono: string | null;
  matricula: string;
  carrera: string;
  semestre: number | null;
  promedio: number | null;
  nivelRiesgo: 'sin_riesgo' | 'bajo' | 'medio' | 'alto' | null;
  condicion: string | null;
  periodo: string;
  sesionesCompletadas: number;
  ultimaSesion: string | null;
}

export interface SesionEstudianteTutor {
  id: number;
  tema: string;
  inicioEn: string;
  duracionMinutos: number;
  observaciones: string | null;
}

export interface ObservacionEstudianteTutor {
  id: number;
  tipo: 'nota' | 'progreso' | 'alerta';
  prioridad: 'normal' | 'alta';
  contenido: string;
  observadoEn: string;
}

export interface DetalleEstudianteTutor {
  estudiante: EstudianteTutor;
  sesiones: SesionEstudianteTutor[];
  observaciones: ObservacionEstudianteTutor[];
}
