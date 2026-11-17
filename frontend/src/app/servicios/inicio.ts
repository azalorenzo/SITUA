import { solicitarApi } from './autenticacion';
import type { ResumenInicio } from '../tipos/inicio';
export function consultarInicio(comprobacion:AbortSignal){return solicitarApi<ResumenInicio>('/coordinacion/inicio',{signal:comprobacion});}
