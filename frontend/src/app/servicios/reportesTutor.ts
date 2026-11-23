import { solicitarApi, solicitarArchivoApi } from './autenticacion';
import type { PanelReportesTutor } from '../tipos/reportesTutor';

// El tutor se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarPanelReportes(comprobacion: AbortSignal) {
  return solicitarApi<PanelReportesTutor>('/tutor/reportes', { signal: comprobacion });
}

export function descargarReporteTutor(id: number) {
  return solicitarArchivoApi(`/tutor/reportes/${id}/pdf`);
}

export function descargarReportesTutor() {
  return solicitarArchivoApi('/tutor/reportes/exportar/pdf');
}
