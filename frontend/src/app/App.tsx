import { mockDashboard, mockDashboardStats } from './mock/data';
import { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { StatCard } from './components/StatCard';


import { RiskChart } from './components/RiskChart';
import { TutorshipChart } from './components/TutorshipChart';
import { ReportsView } from './components/ReportsView';
import { StudentApp } from './components/StudentApp';
import { TutorApp } from './components/TutorApp';
import { TutorsManagementView } from './components/TutorsManagementView';
import { StudentsManagementView } from './components/StudentsManagementView';
import { AssignmentsView } from './components/AssignmentsView';
import { CoordinatorNotificationsView } from './components/CoordinatorNotificationsView';
import { Users, UserCheck, FileText, AlertTriangle, Bell, LogOut, ArrowLeft } from 'lucide-react';
import InicioSesion from './components/InicioSesion';

export default function App() {
  const [logueado, setLogueado] = useState(false);
  const [userRole, setUserRole] = useState<'coordinator' | 'student' | 'tutor'>('coordinator');
  const [activeSection, setActiveSection] = useState('inicio');
  const dashboard = mockDashboard;
  const dashboardStats = mockDashboardStats;
  const logout = () => { setLogueado(false); setActiveSection('inicio'); };
  
  if (!logueado) {
    return <InicioSesion />;
  }
  if (userRole === 'student') {
    return (
      <div>
        <StudentApp onLogout={logout} />
      </div>
    );
  }

  if (userRole === 'tutor') {
    return (
      <div>
        <TutorApp onLogout={logout} />
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-[#F5F7FA] text-[#0B2828]">
      <Sidebar

  activeSection={activeSection}

  onSectionChange={setActiveSection}

  onLogout={logout}

  

/>

      <main className="flex-1 overflow-auto">

        {activeSection === 'inicio' && (

          <header className="bg-white shadow-sm sticky top-0 z-10">

      <div className="px-8 py-4 flex justify-between items-center">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <h2 className="font-semibold">¡Bienvenida, Coordinadora!</h2>
                <p className="text-sm text-[#53716E]">Gestiona tu programa de tutorados</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex gap-2">
                
              </div>
              
              
            
            </div>
          </div>
        </header>
          )}

  <div className="p"></div>

        {/* Dashboard Content */}
        <div className="p-8">
          {activeSection === 'inicio' && (
            <>
              {/* Stats Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <StatCard
                  title="Total estudiantes"
                  value={dashboard?.totalEstudiantes || 0}
                  icon={Users}
                  bgColor="bg-[#F2FBF9]"
                  iconColor="text-[#1F6662]"
                />
                <StatCard
                  title="Tutores activos"
                  value={dashboard?.tutoresActivos || 0}
                  icon={UserCheck}
                  bgColor="bg-[#E4F3ED]"
                  iconColor="text-[#227F78]"
                />
              
                <StatCard
                  title="Estudiantes en riesgo"
                  value={dashboardStats?.pendientes || 0}
                  icon={AlertTriangle}
                  bgColor="bg-[#FCEBED]"
                  iconColor="text-[#B83F4B]"
                />
              </div>

          

              {/* Charts and Alerts Row */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                <div className="lg:col-span-1">
                  <RiskChart dashboardStats={dashboardStats} />
                </div>
                <div className="lg:col-span-2">
                  <TutorshipChart dashboardStats={dashboardStats} /> 
                </div>
              </div>

              {/* Recent Alerts */}
              <div>
              </div>
            </>
          )}

          {activeSection === 'reportes' && (
            <>
              <div className="mb-6">
                <h2 className="font-semibold mb-2">Generador de Reportes</h2>
      
              </div>
              <ReportsView />
            </>
          )}

          {activeSection === 'tutores' && <TutorsManagementView />}
          {activeSection === 'avisos' && <CoordinatorNotificationsView />}

          {activeSection !== 'inicio' &&
           activeSection !== 'reportes' &&
           activeSection !== 'tutores' &&
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
