-- T-186: PIN de 4 números del alumno, guardado SOLO cifrado con BCrypt (60 caracteres).
-- La tablet (módulo estudiante) lo compara con BCryptPasswordEncoder.matches().
ALTER TABLE estudiante ADD COLUMN pin_hash VARCHAR(100);