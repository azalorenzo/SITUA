import { useState } from 'react';

import { StudentSidebar } from './StudentSidebar';

import { StudentView } from './StudentView';
import { StudentAgendaView } from './StudentAgendaView';
import { StudentTutoriasView } from './StudentTutoriasView';
import { StudentMessagesView } from './StudentMessagesView';
import { StudentNotificationsView } from './StudentNotificationsView';

export function StudentApp({ onLogout }: { onLogout: () => void }) {

  const [activeSection, setActiveSection] = useState('inicio');

  const nombreEstudiante =
    "Ana Laura Gómez";

  return (

    <div className="flex h-screen bg-[#F5F7FA] text-[#0B2828]">

      <StudentSidebar onLogout={onLogout}
        activeSection={activeSection}
        onSectionChange={setActiveSection}
        nombreEstudiante={nombreEstudiante || ""}
      />

      <main className="flex-1 overflow-auto">

        {/* HEADER */}
        {activeSection === "inicio" && (

          <header className="bg-white shadow-sm sticky top-0 z-10">

            <div className="px-8 py-4">

              <div>

                <h2 className="font-semibold text-2xl">

                  ¡Hola {nombreEstudiante?.toUpperCase()}! 

                </h2>

              

              </div>

            </div>

          </header>

        )}

        {/* CONTENT */}
        <div className="p-8">

          {activeSection === 'inicio' &&
            <StudentView />
          }

          {activeSection === 'mi-agenda' &&
            <StudentAgendaView />
          }

          {activeSection === 'mis-tutorias' &&
            <StudentTutoriasView />
          }

          {activeSection === 'mensajes' &&
            <StudentMessagesView />
          }

          {activeSection === 'avisos' &&
            <StudentNotificationsView />
          }

          {activeSection !== 'inicio' &&
           activeSection !== 'mi-agenda' &&
           activeSection !== 'mis-tutorias' &&
           activeSection !== 'mensajes' &&
           activeSection !== 'avisos' && (

            <div className="bg-white rounded-lg p-12 text-center shadow-sm">

              <h3 className="font-semibold mb-2">
                Sección en desarrollo
              </h3>

              <p className="text-[#53716E]">
                Esta sección estará disponible próximamente
              </p>

            </div>

          )}

        </div>

      </main>

    </div>
  );
}