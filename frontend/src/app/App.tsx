import { useEffect, useRef, useState } from 'react';
import { cerrarSesionServidor, consultarSesion, guardarSesion, limpiarSesion, obtenerToken, obtenerVencimiento, ErrorSolicitud } from './servicios/autenticacion';
import type { UsuarioSesion, RespuestaInicioSesion } from './tipos/autenticacion';
import { UsuariosAdministracion, TutoriasAdministracion, CatalogosAdministracion } from './components/AdministracionVistas';
import { Sidebar } from './components/Sidebar';
import { InicioCoordinacion } from './components/InicioCoordinacion';


import { ReportsView } from './components/ReportsView';
import { StudentApp } from './components/StudentApp';
import { TutorApp } from './components/TutorApp';
import { TutorsManagementView } from './components/TutorsManagementView';
import { StudentsManagementView } from './components/StudentsManagementView';
import { AssignmentsView } from './components/AssignmentsView';
import { CoordinatorNotificationsView } from './components/CoordinatorNotificationsView';
import { BarraCoordinacion } from './components/BarraCoordinacion';
import InicioSesion from './components/InicioSesion';

export default function App() {
  const [usuario, cambiarUsuario] = useState<UsuarioSesion | null>(null);
  const [revisandoSesion, cambiarRevisandoSesion] = useState(true);
  const [mensajeSesion, cambiarMensajeSesion] = useState('');
  const [vencimiento, cambiarVencimiento] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState('inicio');
  const [menuAbierto,cambiarMenu]=useState(false),[cerrando,cambiarCierre]=useState(false),[errorCierre,cambiarErrorCierre]=useState('');
  const cierreEnCurso=useRef(false);

  useEffect(() => {
    let vigente = true;
    if (!obtenerToken()) { cambiarRevisandoSesion(false); return; }
    consultarSesion().then(datos => {
      if (vigente) { cambiarUsuario(datos); cambiarVencimiento(obtenerVencimiento()); }
    }).catch(error => {
      if (!vigente) return;
      if (error instanceof ErrorSolicitud && (error.estado === 401 || error.estado === 403)) limpiarSesion();
      cambiarMensajeSesion(error instanceof Error ? error.message : 'No se pudo recuperar la sesión.');
    }).finally(() => { if (vigente) cambiarRevisandoSesion(false); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!usuario || !vencimiento) return;
    const tiempo = Math.max(0, Date.parse(vencimiento) - Date.now());
    const aviso = window.setTimeout(() => {
      limpiarSesion(); cambiarUsuario(null); cambiarVencimiento(null); setActiveSection('inicio');
      cambiarMensajeSesion('Tu sesión venció. Inicia sesión nuevamente.');
    }, tiempo);
    return () => window.clearTimeout(aviso);
  }, [usuario, vencimiento]);

  const limpiarAcceso = () => {
    cambiarMenu(false);
    limpiarSesion(); cambiarUsuario(null); cambiarVencimiento(null);
    cambiarMensajeSesion(''); setActiveSection('inicio');
  };
  const cerrarSesion=async()=>{
    if(cierreEnCurso.current)return;cierreEnCurso.current=true;cambiarCierre(true);cambiarErrorCierre('');
    try{await cerrarSesionServidor();limpiarAcceso();}
    catch(fallo){if(fallo instanceof ErrorSolicitud&&fallo.estado===401)limpiarAcceso();else cambiarErrorCierre(fallo instanceof Error?fallo.message:'No se pudo cerrar la sesión.');}
    finally{cierreEnCurso.current=false;cambiarCierre(false);}
  };
  const avisoCierre=errorCierre?<div role="alert" className="fixed top-3 left-1/2 -translate-x-1/2 z-[100] w-[calc(100%-2rem)] max-w-md rounded-lg border border-[#B83F4B] bg-white text-[#B83F4B] p-4 shadow-lg"><p>{errorCierre}</p><button type="button" disabled={cerrando} className="mt-2 underline" onClick={cerrarSesion}>Reintentar</button></div>:null;
  const recibirSesion = (respuesta: RespuestaInicioSesion, recordar: boolean) => {
    guardarSesion(respuesta, recordar); cambiarUsuario(respuesta.usuario);
    cambiarVencimiento(respuesta.venceEn); cambiarMensajeSesion(''); setActiveSection('inicio');
  };
  const rolUsuario = usuario?.roles.includes('coordinador') ? 'coordinator'
    : usuario?.roles.includes('tutor') ? 'tutor'
    : usuario?.roles.includes('estudiante') ? 'student' : null;

  if (revisandoSesion) return <main className="inicio-sesion"><p className="cargando-sesion" role="status">Comprobando tu sesión…</p></main>;
  if (!usuario || !rolUsuario) return <InicioSesion alIngresar={recibirSesion} mensajeInicial={mensajeSesion} />;
  if (rolUsuario === 'student') {
    return (
      <div>
        {avisoCierre}<StudentApp onLogout={cerrarSesion} usuario={usuario} cerrando={cerrando}/>
      </div>
    );
  }

  if (rolUsuario === 'tutor') {
    return (
      <div>
        {avisoCierre}<TutorApp onLogout={cerrarSesion} usuario={usuario} cerrando={cerrando}/>
      </div>
    );
  }

  return (
    <div className="flex h-dvh bg-[#F5F7FA] text-[#0B2828]">
      {avisoCierre}<Sidebar activeSection={activeSection} onSectionChange={setActiveSection} abierto={menuAbierto} alCerrar={()=>cambiarMenu(false)}/>
      <main className="flex-1 min-w-0 overflow-auto">
        <BarraCoordinacion usuario={usuario} alCerrarSesion={cerrarSesion} cerrando={cerrando} alAbrirMenu={()=>cambiarMenu(true)} menuAbierto={menuAbierto}/>

        {/* Aquí se muestra la sección elegida en el menú. */}
        <div className="p-4 sm:p-8">
          {activeSection === 'inicio' && <InicioCoordinacion />}
          {activeSection === 'usuarios' && <UsuariosAdministracion />}
          {activeSection === 'tutorias' && <TutoriasAdministracion />}
          {activeSection === 'catalogos' && <CatalogosAdministracion />}

          {activeSection === 'reportes' && <ReportsView />}

          {activeSection === 'tutores' && <TutorsManagementView />}
          {activeSection === 'estudiantes' && <StudentsManagementView />}
          {activeSection === 'asignaciones' && <AssignmentsView />}
          {activeSection === 'avisos' && <CoordinatorNotificationsView />}

          {!['usuarios','tutorias','catalogos'].includes(activeSection) && activeSection !== 'inicio' &&
           activeSection !== 'reportes' &&
           activeSection !== 'tutores' &&
           activeSection !== 'estudiantes' &&
           activeSection !== 'asignaciones' &&
           activeSection !== 'avisos' && (
            <div className="bg-white rounded-lg p-12 text-center shadow-sm">
              <h3 className="font-semibold mb-2">Sección en desarrollo</h3>
              <p className="text-[#53716E]">Esta sección estará disponible próximamente</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
