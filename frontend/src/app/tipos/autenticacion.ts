export interface UsuarioSesion {
  id: number;
  nombre: string;
  correo: string;
  avatarUrl: string | null;
  roles: string[];
}

export interface RespuestaInicioSesion {
  token: string;
  tipoToken: 'Bearer';
  venceEn: string;
  usuario: UsuarioSesion;
}

export type ErroresFormulario = Record<string, string[]>;

export interface DatosRegistro {
  nombre: string;
  matricula: string;
  correo: string;
  contrasena: string;
  confirmarContrasena: string;
}
