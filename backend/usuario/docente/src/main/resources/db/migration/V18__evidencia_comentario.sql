-- T-383 (US-34): el docente deja un comentario de retroalimentación en la
-- evidencia para orientar al alumno en su mejora. Es opcional y se guarda
-- cuando el docente lo escribe (T-384).
ALTER TABLE evidencia
    ADD COLUMN comentario TEXT;
