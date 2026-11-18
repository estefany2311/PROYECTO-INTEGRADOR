package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.Transcripcion;

// T-415: convierte el audio de la lectura en texto (en las pruebas se reemplaza por uno falso)
public interface ClienteVoz {

    Transcripcion transcribir(byte[] audioWav);
}
