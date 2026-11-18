// Tal como lo devuelve el backend: fechas en UTC sin zona, igual que el resto del
// proyecto (ver asignaciones). El servicio de horarios se encarga de convertirlas.
export interface Horario {
  id: number;
  inicioEn: string;
  finEn: string;
  modalidad: 'presencial' | 'virtual';
  ubicacion: string | null;
  enlaceVirtual: string | null;
  estado: 'habilitada' | 'bloqueada';
  estudianteNombre: string | null;
}

export interface DatosHorario {
  inicioEn: string;
  finEn: string;
  ubicacion: string;
}
