import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Alumnos from "./pages/Alumnos";
import FormularioAula from "./FormularioAula";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/alumnos" replace />} />
        <Route path="/aulas" element={<FormularioAula />} />
        <Route path="/alumnos" element={<Alumnos />} />
      </Route>
    </Routes>
  );
}