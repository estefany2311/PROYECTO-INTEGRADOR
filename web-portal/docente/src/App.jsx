import { useSyncExternalStore } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Alumnos from "./pages/Alumnos";
import Biblioteca from "./pages/Biblioteca";
import Catalogo from "./pages/Catalogo";
import Dashboard from "./pages/Dashboard";
import DetalleEstudiante from "./pages/DetalleEstudiante";
import DetalleEvidencia from "./pages/DetalleEvidencia";
import Evidencias from "./pages/Evidencias";
import Grupos from "./pages/Grupos";
import Login from "./pages/Login";
import Tareas from "./pages/Tareas";
import FormularioAula from "./FormularioAula";
import { obtenerSesion, suscribirSesion } from "./sesion";

export default function App() {
  const sesion = useSyncExternalStore(suscribirSesion, obtenerSesion);
  const docenteId = sesion?.docente?.id;

  // Sin sesión, cualquier dirección lleva al login.
  if (!docenteId) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    // la clave hace que al cambiar de docente no quede nada del anterior
    <Routes key={docenteId}>
      <Route path="/login" element={<Navigate to="/alumnos" replace />} />
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/alumnos" replace />} />
        <Route path="/aulas" element={<FormularioAula />} />
        <Route path="/alumnos" element={<Alumnos />} />
        <Route path="/grupos" element={<Grupos />} />
        <Route path="/tareas" element={<Tareas />} />
        <Route path="/biblioteca" element={<Biblioteca />} />
        <Route path="/catalogo" element={<Catalogo />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/evidencias" element={<Evidencias />} />
        <Route
          path="/evidencias/:aulaId/:evidenciaId"
          element={<DetalleEvidencia />}
        />
        <Route
          path="/dashboard/aulas/:aulaId/estudiantes/:estudianteId"
          element={<DetalleEstudiante />}
        />
      </Route>
    </Routes>
  );
}