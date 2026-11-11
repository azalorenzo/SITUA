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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./ui/select";

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface Sesion {
  id_sesion: string;
  id_alumno: string;
  nombre_alumno: string;
  fecha: string;
  tema: string;
  notas: string;
  estatus: string;
}

interface Alumno {
  id_alumno: string;
  nombre: string;
  apellido: string;
}

interface SesionForm {
  id_alumno: string;
  fecha: string;
  tema: string;
  notas: string;
}


// ─── Componente ───────────────────────────────────────────────────────────────

export default function Sessions() {
  // Lista de sesiones
  const [sesiones, setSesiones] = useState<Sesion[]>(mockSessions);
  const [cargando, setCargando] = useState(false);

  // Alumnos para el select del formulario
  const [alumnos, setAlumnos] = useState<Alumno[]>(mockStudents);

  // Control del modal
  const [modalAbierto, setModalAbierto] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [mensajeExito, setMensajeExito] = useState("");
  const [mensajeError, setMensajeError] = useState("");

  // Campos del formulario
  const [form, setForm] = useState<SesionForm>({
    id_alumno: "",
    fecha: "",
    tema: "",
    notas: "",
  });

  // ── Abrir modal ──────────────────────────────────────────────────────────
  function abrirModal() {
    setForm({ id_alumno: "", fecha: "", tema: "", notas: "" });
    setMensajeExito("");
    setMensajeError("");
    setModalAbierto(true);
  }

  // ── Guardar sesión ────────────────────────────────────────────────────────
  function guardarSesion() {
    if (!form.id_alumno || !form.fecha || !form.tema.trim()) {
      setMensajeError("Alumno, fecha y tema son obligatorios.");
      return;
    }

    const alumno = alumnos.find(a => a.id_alumno === form.id_alumno);
    setSesiones(prev => [...prev, { ...form, id_sesion: String(Date.now()), nombre_alumno: alumno ? alumno.nombre + ' ' + alumno.apellido : '', estatus: 'Programada' }]);
    setMensajeError(''); setModalAbierto(false);
  }

  const formatearFecha = (fecha: string) => new Date(fecha + 'T00:00:00').toLocaleDateString('es-MX');
  const colorEstatus = (estatus: string) => estatus === 'Completada' ? 'text-green-700' : 'text-amber-700';

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Sesiones</h1>

        {/* ✅ BOTÓN AGREGAR SESIÓN — onClick conectado */}
        <Button onClick={abrirModal}>+ Sesión</Button>
      </div>

      {cargando && (
        <p className="text-muted-foreground">Cargando sesiones...</p>
      )}

      {!cargando && sesiones.length === 0 && (
        <p className="text-muted-foreground">No hay sesiones registradas.</p>
      )}

      {/* ── Tabla de sesiones ── */}
      {!cargando && sesiones.length > 0 && (
        <div className="rounded-md border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Alumno</th>
                <th className="px-4 py-3 text-left font-medium">Fecha</th>
                <th className="px-4 py-3 text-left font-medium">Tema</th>
                <th className="px-4 py-3 text-left font-medium">Notas</th>
                <th className="px-4 py-3 text-left font-medium">Estatus</th>
              </tr>
            </thead>
            <tbody>
              {sesiones.map((sesion) => (
                <tr
                  key={sesion.id_sesion}
                  className="border-t hover:bg-muted/30"
                >
                  <td className="px-4 py-3">{sesion.nombre_alumno}</td>
                  <td className="px-4 py-3">{formatearFecha(sesion.fecha)}</td>
                  <td className="px-4 py-3">{sesion.tema}</td>
                  <td className="px-4 py-3 max-w-xs truncate">{sesion.notas || "—"}</td>
                  <td className={`px-4 py-3 ${colorEstatus(sesion.estatus)}`}>
                    {sesion.estatus || "Pendiente"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* ── Modal agregar sesión ── */}
      <Dialog open={modalAbierto} onOpenChange={setModalAbierto}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Nueva sesión</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {/* Alumno */}
            <div className="space-y-1">
              <Label>Alumno *</Label>
              <Select
                value={form.id_alumno}
                onValueChange={(val) => setForm({ ...form, id_alumno: val })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Selecciona un alumno" />
                </SelectTrigger>
                <SelectContent>
                  {alumnos.map((a) => (
                    <SelectItem key={a.id_alumno} value={a.id_alumno}>
                      {a.nombre} {a.apellido}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Fecha */}
            <div className="space-y-1">
              <Label htmlFor="s-fecha">Fecha *</Label>
              <Input
                id="s-fecha"
                type="date"
                value={form.fecha}
                onChange={(e) => setForm({ ...form, fecha: e.target.value })}
              />
            </div>

            {/* Tema */}
            <div className="space-y-1">
              <Label htmlFor="s-tema">Tema *</Label>
              <Input
                id="s-tema"
                placeholder="Ej: Derivadas e integrales"
                value={form.tema}
                onChange={(e) => setForm({ ...form, tema: e.target.value })}
              />
            </div>

            {/* Notas */}
            <div className="space-y-1">
              <Label htmlFor="s-notas">Notas</Label>
              <Textarea
                id="s-notas"
                placeholder="Observaciones de la sesión..."
                rows={3}
                value={form.notas}
                onChange={(e) => setForm({ ...form, notas: e.target.value })}
              />
            </div>

            {/* Feedback */}
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
            <Button onClick={guardarSesion} disabled={guardando}>
              {guardando ? "Guardando..." : "Guardar sesión"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
