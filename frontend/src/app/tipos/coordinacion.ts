export interface PersonaCoordinacion {
  id: number;
  usuarioId: number;
  nombre: string;
  correo: string;
  telefono: string | null;
  avatarUrl: string | null;
  estado: 'activo' | 'inactivo';
}

export interface TutorCoordinacion extends PersonaCoordinacion {
  departamentoId: number;
  departamento: string;
}

export interface EstudianteCoordinacion extends PersonaCoordinacion {
  matricula: string;
  periodoId: number | null;
  periodo: string | null;
  carrera: string | null;
  semestre: number | null;
  promedio: number | null;
  nivelRiesgo: 'sin_riesgo' | 'bajo' | 'medio' | 'alto' | null;
}

export interface Departamento {
  id: number; nombre: string; activo: boolean;
}
export interface FiltrosTutores {
  pagina: number; tamano: number; busqueda: string; estado: string; departamentoId: string;
}
export interface PaginaTutores {
  contenido: TutorCoordinacion[]; pagina: number; tamano: number; totalRegistros: number; totalPaginas: number;
  resumen: { total: number; activos: number; inactivos: number };
}
