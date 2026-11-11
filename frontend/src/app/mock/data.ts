// Datos ficticios del mockup, sin persistencia ni servicios externos.
export const mockTutors = [
 { id_tutor: '1', nombre: 'Javier López García', departamento: 'Ciencias Básicas', correo: 'javier@example.com' },
 { id_tutor: '2', nombre: 'Elena Martínez Ruiz', departamento: 'Sistemas', correo: 'elena@example.com' },
 { id_tutor: '3', nombre: 'Luis Hernández Cruz', departamento: 'Ingeniería Industrial', correo: 'luis@example.com' },
];
export const mockStudents = [
 { id_alumno: '1', nombre: 'Ana Laura', apellido: 'Gómez', matricula: '22160001', carrera: 'Ingeniería en Sistemas', semestre: '4', correo: 'ana@example.com' },
 { id_alumno: '2', nombre: 'Carlos', apellido: 'Méndez Ruiz', matricula: '22160002', carrera: 'Ingeniería Industrial', semestre: '5', correo: 'carlos@example.com' },
 { id_alumno: '3', nombre: 'María', apellido: 'Fernández López', matricula: '22160003', carrera: 'Arquitectura', semestre: '3', correo: 'maria@example.com' },
];
export const mockReports = [
 { id_reporte: '1', estudiante: 'Ana Laura Gómez', tutor: 'Javier López García', fecha: '2026-06-10', estatus: 'Pendiente', motivo: 'Bajo rendimiento' },
 { id_reporte: '2', estudiante: 'Carlos Méndez Ruiz', tutor: 'Elena Martínez Ruiz', fecha: '2026-06-12', estatus: 'Completado', motivo: 'Seguimiento académico' },
 { id_reporte: '3', estudiante: 'María Fernández López', tutor: 'Luis Hernández Cruz', fecha: '2026-06-15', estatus: 'Pendiente', motivo: 'Ausentismo' },
];
export const mockNotices = [
 { id_aviso: '1', titulo: 'Periodo de tutorías', descripcion: 'Consulta los horarios disponibles y programa tu siguiente sesión.', prioridad: 'Alta', categoria: 'Académico', fecha: '2026-06-01T09:00:00', leido: false },
 { id_aviso: '2', titulo: 'Taller de hábitos de estudio', descripcion: 'Participa en el taller del viernes en la biblioteca.', prioridad: 'Media', categoria: 'Taller', fecha: '2026-06-02T12:00:00', leido: false },
 { id_aviso: '3', titulo: 'Reporte del periodo', descripcion: 'Consulta el resumen de actividades.', prioridad: 'Normal', categoria: 'General', fecha: '2026-06-03T10:00:00', leido: true },
];
export const mockSessions = [
 { id_sesion: '1', id_alumno: '1', nombre_alumno: 'Ana Laura Gómez', tema: 'Cálculo diferencial', tutor: 'Javier López García', nombre_tutor: 'Javier López García', fecha: '2026-06-10', hora: '10:00', lugar: 'Aula 301', estado: 'Confirmada', estatus: 'Programada', notas: 'Repaso de derivadas' },
 { id_sesion: '2', id_alumno: '2', nombre_alumno: 'Carlos Méndez Ruiz', tema: 'Programación', tutor: 'Elena Martínez Ruiz', nombre_tutor: 'Elena Martínez Ruiz', fecha: '2026-06-15', hora: '12:00', lugar: 'Laboratorio 2', estado: 'Pendiente', estatus: 'Programada', notas: 'Estructuras de control' },
];
export const mockDashboard = { totalEstudiantes: 523, tutoresActivos: 38 };
export const mockDashboardStats = { pendientes: 38, motivos: [{ motivo: 'Bajo rendimiento', total: 18 }, { motivo: 'Ausentismo', total: 12 }, { motivo: 'Seguimiento', total: 8 }], mensual: Array.from({ length: 12 }, (_, i) => ({ mes: i + 1, total: 20 + i * 4 })) };
export function showMockAction() { window.alert('Vista de demostración: esta acción no genera archivos ni envía información.'); }
