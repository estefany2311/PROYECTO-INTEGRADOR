-- T-388 (US-38): fecha límite y estado de seguimiento de cada tarea asignada.
-- fecha_limite es opcional: una tarea sin fecha nunca vence.
-- estado guarda solo lo que se registra: 'pendiente' o 'entregada'. "Vencida" no
-- se guarda: se calcula (pendiente con la fecha límite ya pasada), así no hay
-- que actualizar filas cuando pasa el tiempo.
-- entregada_en es el momento en que el alumno entregó; permite saber si
-- entregó a tiempo.
ALTER TABLE asignacion
    ADD COLUMN fecha_limite DATE,
    ADD COLUMN estado       VARCHAR(10) NOT NULL DEFAULT 'pendiente'
        CHECK (estado IN ('pendiente', 'entregada')),
    ADD COLUMN entregada_en TIMESTAMP,
    -- coherencia: solo una tarea entregada tiene fecha de entrega
    ADD CONSTRAINT chk_asignacion_entrega CHECK (
        (estado = 'entregada' AND entregada_en IS NOT NULL) OR
        (estado = 'pendiente' AND entregada_en IS NULL)
    );

-- seguimiento: tareas con fecha límite por vencer o vencidas
CREATE INDEX idx_asignacion_fecha_limite ON asignacion (fecha_limite);
