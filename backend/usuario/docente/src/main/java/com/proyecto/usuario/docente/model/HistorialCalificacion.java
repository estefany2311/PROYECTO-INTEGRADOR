package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-380 (US-33): historial de calificaciones de una evidencia.
 *
 * <p>Cada aprobación o corrección del docente deja una fila con la nota de la
 * IA, la nota final que quedó, el motivo cuando hubo corrección y quién revisó.
 * La evidencia conserva solo la última revisión; esta tabla conserva todas para
 * poder comparar la nota de la IA con la del docente.</p>
 */
@Entity
@Table(name = "historial_calificacion")
public class HistorialCalificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidencia_id")
    private Evidencia evidencia;

    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    // Nota que había dado la IA; nula si la evidencia no tenía evaluación
    @Column(name = "nota_ia")
    private Short notaIa;

    @Column(name = "nota_docente", nullable = false)
    private Short notaDocente;

    // Solo las correcciones llevan motivo; las aprobaciones lo dejan nulo
    @Column(columnDefinition = "text")
    private String motivo;

    // `aprobada` o `corregida`, igual que el estado de la evidencia
    @Column(nullable = false, length = 15)
    private String accion;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public HistorialCalificacion() {
    }

    /**
     * Registra una revisión del docente sobre la evidencia.
     */
    public static HistorialCalificacion registrar(Evidencia evidencia, Long docenteId,
                                                  String accion, Short notaIa,
                                                  Short notaDocente, String motivo) {
        HistorialCalificacion historial = new HistorialCalificacion();
        historial.evidencia = evidencia;
        historial.docenteId = docenteId;
        historial.accion = accion;
        historial.notaIa = notaIa;
        historial.notaDocente = notaDocente;
        historial.motivo = motivo;
        return historial;
    }

    public Long getId() { return id; }
    public Evidencia getEvidencia() { return evidencia; }
    public Long getDocenteId() { return docenteId; }
    public Short getNotaIa() { return notaIa; }
    public Short getNotaDocente() { return notaDocente; }
    public String getMotivo() { return motivo; }
    public String getAccion() { return accion; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
