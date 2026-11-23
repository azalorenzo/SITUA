// Conversación directa del tutor conectado. Las fechas llegan en UTC sin zona.
export interface ConversacionTutor {
  id: number;
  nombre: string;
  avatarUrl: string | null;
  ultimoMensaje: string | null;
  ultimoEnviadoEn: string | null;
  noLeidos: number;
}

export interface MensajeTutor {
  id: number;
  contenido: string;
  enviadoEn: string;
  enviado: boolean;
}

export interface ConversacionLeida {
  id: number;
}
