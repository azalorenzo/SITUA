import { solicitarApi } from './autenticacion';
import type { UsuarioSesion } from '../tipos/autenticacion';

export interface PerfilUsuario {
  usuario: UsuarioSesion;
  rol: 'coordinador' | 'tutor' | 'estudiante';
  puedeEditarCorreo: boolean;
  telefono: string | null;
  matricula: string | null;
  carreraId: number | null;
  periodoId: number | null;
  periodo: string | null;
  semestre: number | null;
  puedeEditarAcademico: boolean;
  departamentoId: number | null;
  aceptaSms: boolean;

  carreras: { id: number; nombre: string }[];
  departamentos: { id: number; nombre: string }[];
}
export interface DatosEditarPerfil {
  nombre: string;
  correo: string;
  telefono: string;
  matricula: string | null;
  carreraId: number | null;
  periodoId: number | null;
  semestre: number | null;
  departamentoId: number | null;
  aceptaSms: boolean;

}
export const consultarPerfil = (signal: AbortSignal) => solicitarApi<PerfilUsuario>('/perfil', { signal });
export const guardarPerfil = (datos: DatosEditarPerfil) => solicitarApi<PerfilUsuario>('/perfil', { method: 'PUT', body: JSON.stringify(datos) });
