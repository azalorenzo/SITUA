import { useState } from 'react';
import { TutorSidebar } from './TutorSidebar';
import { TutorView } from './TutorView';
import { MyStudentsView } from './MyStudentsView';
import { ScheduleView } from './ScheduleView';
import { CompletedSessionsView } from './CompletedSessionsView';
import { TutorReportsView } from './TutorReportsView';
import { TutorNotificationsView } from './TutorNotificationsView';
import { MessagesView } from './MessagesView';

export function TutorApp({ onLogout }: { onLogout: () => void }) {
  const [activeSection, setActiveSection] = useState('inicio');
  const nombreTutor = "Javier López García";

  return (
    <div className="flex h-screen bg-[#F5F7FA] text-[#0B2828]">
      <TutorSidebar onLogout={onLogout}
        activeSection={activeSection}
        onSectionChange={setActiveSection}
        nombreTutor={nombreTutor}
      />

      <main className="flex-1 overflow-auto">
        {/* Header solo en inicio */}
        {activeSection === 'inicio' && (
          <header className="bg-white shadow-sm sticky top-0 z-10">
            <div className="px-8 py-4 flex justify-between items-center">
              <div>
                <h2 className="font-semibold">¡Hola {nombreTutor.toUpperCase()}!</h2>
                <p className="text-sm text-[#53716E]">Gestiona tus estudiantes y tutorías</p>
              </div>
            </div>
          </header>
        )}

        {/* Content */}
        <div className="p-8">
          {activeSection === 'inicio'             && <TutorView onSectionChange={setActiveSection} />}
          {activeSection === 'mis-estudiantes'    && <MyStudentsView />}
          {activeSection === 'horarios'           && <ScheduleView />}
          {activeSection === 'sesiones-realizadas'&& <CompletedSessionsView />}
          {activeSection === 'reportes'           && <TutorReportsView />}
          {activeSection === 'mensajes'           && <MessagesView />}
          {activeSection === 'avisos'             && <TutorNotificationsView />}

          {!['inicio','mis-estudiantes','horarios','sesiones-realizadas','reportes','mensajes','avisos']
            .includes(activeSection) && (
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
