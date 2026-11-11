import { mockStudents, mockSessions } from '../mock/data';
import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "./ui/dialog";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Textarea } from "./ui/textarea";

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface Alumno {
  id_alumno: string;
  nombre: string;
  apellido: string;
  matricula: string;
  carrera: string;
  semestre: string;
  correo: string;
}

interface EventoForm {
  titulo: string;
  descripcion: string;
  fecha: string;
}


// ─── Componente ───────────────────────────────────────────────────────────────

export default function Students() {
  // Lista de alumnos
  const [alumnos, setAlumnos] = useState<Alumno[]>(mockStudents);
  const [cargando, setCargando] = useState(false);

  // Control del modal de evento
  const [modalAbierto, setModalAbierto] = useState(false);
  const [alumnoSeleccionado, setAlumnoSeleccionado] = useState<Alumno | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [mensajeExito, setMensajeExito] = useState("");
  const [mensajeError, setMensajeError] = useState("");

  // Campos del formulario de evento
  const [form, setForm] = useState<EventoForm>({
    titulo: "",
    descripcion: "",
    fecha: "",
  });

  // ── Abrir modal para un alumno específico ────────────────────────────────
  function abrirModalEvento(alumno: Alumno) {
    setAlumnoSeleccionado(alumno);
    setForm({ titulo: "", descripcion: "", fecha: "" });
    setMensajeExito("");
    setMensajeError("");
    setModalAbierto(true);
  }

  // ── Simular registro de evento ──────────────────────────────────────────────
  function guardarEvento() {
    if (!alumnoSeleccionado) return;

    if (!form.titulo.trim() || !form.fecha) {
      setMensajeError("El título y la fecha son obligatorios.");
      return;
    }

    setMensajeError(''); setMensajeExito('Evento de demostración registrado.');
    setTimeout(() => { setModalAbierto(false); setMensajeExito(''); }, 1500);
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Estudiantes</h1>
      </div>

      {cargando && (
        <p className="text-muted-foreground">Cargando estudiantes...</p>
      )}

      {!cargando && alumnos.length === 0 && (
        <p className="text-muted-foreground">No hay estudiantes registrados.</p>
      )}

      {/* ── Tabla de alumnos ── */}
      {!cargando && alumnos.length > 0 && (
        <div className="rounded-md border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Nombre</th>
                <th className="px-4 py-3 text-left font-medium">Matrícula</th>
                <th className="px-4 py-3 text-left font-medium">Carrera</th>
                <th className="px-4 py-3 text-left font-medium">Semestre</th>
                <th className="px-4 py-3 text-left font-medium">Correo</th>
                <th className="px-4 py-3 text-left font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {alumnos.map((alumno) => (
                <tr key={alumno.id_alumno} className="border-t hover:bg-muted/30">
                  <td className="px-4 py-3">
                    {alumno.nombre} {alumno.apellido}
                  </td>
                  <td className="px-4 py-3">{alumno.matricula}</td>
                  <td className="px-4 py-3">{alumno.carrera}</td>
                  <td className="px-4 py-3">{alumno.semestre}</td>
                  <td className="px-4 py-3">{alumno.correo}</td>
                  <td className="px-4 py-3">
                    {/* ✅ BOTÓN AGREGAR EVENTO — onClick conectado */}
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => abrirModalEvento(alumno)}
                    >
                      + Evento
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* ── Modal agregar evento ── */}
      <Dialog open={modalAbierto} onOpenChange={setModalAbierto}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              Agregar evento
              {alumnoSeleccionado && (
                <span className="font-normal text-muted-foreground ml-2 text-base">
                  — {alumnoSeleccionado.nombre} {alumnoSeleccionado.apellido}
                </span>
              )}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {/* Título */}
            <div className="space-y-1">
              <Label htmlFor="titulo">Título *</Label>
              <Input
                id="titulo"
                placeholder="Ej: Asesoría de cálculo"
                value={form.titulo}
                onChange={(e) => setForm({ ...form, titulo: e.target.value })}
              />
            </div>

            {/* Fecha */}
            <div className="space-y-1">
              <Label htmlFor="fecha">Fecha *</Label>
              <Input
                id="fecha"
                type="date"
                value={form.fecha}
                onChange={(e) => setForm({ ...form, fecha: e.target.value })}
              />
            </div>

            {/* Descripción */}
            <div className="space-y-1">
              <Label htmlFor="descripcion">Descripción</Label>
              <Textarea
                id="descripcion"
                placeholder="Notas adicionales sobre el evento..."
                rows={3}
                value={form.descripcion}
                onChange={(e) =>
                  setForm({ ...form, descripcion: e.target.value })
                }
              />
            </div>

            {/* Mensajes de retroalimentación */}
            {mensajeExito && (
              <p className="text-sm text-[#227F78] font-medium">{mensajeExito}</p>
            )}
            {mensajeError && (
              <p className="text-sm text-[#B83F4B]">{mensajeError}</p>
            )}
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setModalAbierto(false)}
              disabled={guardando}
            >
              Cancelar
            </Button>
            <Button onClick={guardarEvento} disabled={guardando}>
              {guardando ? "Guardando..." : "Guardar evento"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
