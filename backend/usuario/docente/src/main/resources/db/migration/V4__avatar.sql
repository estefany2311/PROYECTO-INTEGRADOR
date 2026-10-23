-- T-185: catálogo de 12 avatares y el avatar elegido para cada alumno.
CREATE TABLE avatar (
    id     SMALLSERIAL  PRIMARY KEY,
    nombre VARCHAR(40)  NOT NULL UNIQUE,
    url    VARCHAR(255) NOT NULL
);

INSERT INTO avatar (nombre, url) VALUES
('Zorro',     'https://api.dicebear.com/9.x/adventurer/png?seed=Zorro&backgroundColor=ffd5dc'),
('Búho',      'https://api.dicebear.com/9.x/adventurer/png?seed=Buho&backgroundColor=c0f5d8'),
('Delfín',    'https://api.dicebear.com/9.x/adventurer/png?seed=Delfin&backgroundColor=b6e3f4'),
('León',      'https://api.dicebear.com/9.x/adventurer/png?seed=Leon&backgroundColor=ffe8b6'),
('Tortuga',   'https://api.dicebear.com/9.x/adventurer/png?seed=Tortuga&backgroundColor=c0f5d8'),
('Mariposa',  'https://api.dicebear.com/9.x/adventurer/png?seed=Mariposa&backgroundColor=e0c3fc'),
('Cóndor',    'https://api.dicebear.com/9.x/adventurer/png?seed=Condor&backgroundColor=b6e3f4'),
('Llama',     'https://api.dicebear.com/9.x/adventurer/png?seed=Llama&backgroundColor=ffd5dc'),
('Panda',     'https://api.dicebear.com/9.x/adventurer/png?seed=Panda&backgroundColor=ffe8b6'),
('Gato',      'https://api.dicebear.com/9.x/adventurer/png?seed=Gato&backgroundColor=e0c3fc'),
('Colibrí',   'https://api.dicebear.com/9.x/adventurer/png?seed=Colibri&backgroundColor=c0f5d8'),
('Oso',       'https://api.dicebear.com/9.x/adventurer/png?seed=Oso&backgroundColor=b6e3f4');

-- Avatar elegido por el docente para cada alumno (opcional hasta que lo asigne)
ALTER TABLE estudiante ADD COLUMN avatar_id SMALLINT REFERENCES avatar (id);