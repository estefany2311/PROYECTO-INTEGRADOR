import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Alumnos from "./pages/Alumnos";
import Catalogo from "./pages/Catalogo";
import Dashboard from "./pages/Dashboard";
import Grupos from "./pages/Grupos";
import FormularioAula from "./FormularioAula";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/alumnos" replace />} />
        <Route path="/aulas" element={<FormularioAula />} />
        <Route path="/alumnos" element={<Alumnos />} />
        <Route path="/grupos" element={<Grupos />} />
        <Route path="/catalogo" element={<Catalogo />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Route>
    </Routes>
  );
}