import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Alumnos from "./pages/Alumnos";
import Catalogo from "./pages/Catalogo";
import FormularioAula from "./FormularioAula";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/alumnos" replace />} />
        <Route path="/aulas" element={<FormularioAula />} />
        <Route path="/alumnos" element={<Alumnos />} />
        <Route path="/catalogo" element={<Catalogo />} />
      </Route>
    </Routes>
  );
}