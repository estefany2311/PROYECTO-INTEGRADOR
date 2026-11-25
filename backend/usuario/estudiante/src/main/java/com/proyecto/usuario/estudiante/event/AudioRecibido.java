package com.proyecto.usuario.estudiante.event;

// T-416: se publica al guardar la evidencia; el procesador lo atiende cuando la transacción ya terminó
public record AudioRecibido(long evidenciaId, String clave) {}
