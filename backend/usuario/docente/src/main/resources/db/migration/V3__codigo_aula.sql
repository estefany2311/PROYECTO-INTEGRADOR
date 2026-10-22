-- Código corto que el niño escribe en la tablet para encontrar su aula.
ALTER TABLE aula ADD COLUMN codigo VARCHAR(10);

-- Aulas existentes: 6 caracteres aleatorios; translate cambia 0→G y 1→H para evitar confusiones
UPDATE aula
SET codigo = translate(upper(substr(md5(random()::text || id::text), 1, 6)), '01', 'GH')
WHERE codigo IS NULL;

ALTER TABLE aula ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE aula ADD CONSTRAINT uq_aula_codigo UNIQUE (codigo);