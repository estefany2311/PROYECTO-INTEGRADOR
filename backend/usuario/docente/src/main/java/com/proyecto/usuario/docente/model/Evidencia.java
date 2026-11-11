package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-296 (US-31): evidencia de audio o texto que el docente debe revisar.
 *
 * <p>La tabla {@code evidencia} la comparten los módulos docente y estudiante:
 * Karla registra la evidencia desde la tablet y el docente la revisa en US-31.
 * Una evidencia de audio guarda su URL y una de texto su contenido; la base
 * valida con un CHECK que coincida el tipo con el contenido.</p>
 */
@Entity
@Table(name = "evidencia")
public class Evidencia {

    // valores permitidos; coinciden con los CHECK de la migración V10
    public static final String AUDIO = "audio";
    public static final String TEXTO = "texto";
    public static final String PENDIENTE = "pendiente";
    public static final String APROBADA = "aprobada";
    public static final String RECHAZADA = "rechazada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id")
    private Nivel nivel;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    // columnDefinition evita que Hibernate valide `text` como si fuera varchar
    @Column(columnDefinition = "text")
    private String texto;

    @Column(name = "resultado_ia", columnDefinition = "text")
    private String resultadoIa;

    @Column(name = "estado_revision", nullable = false)
    private String estadoRevision = PENDIENTE;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    public Evidencia() {
    }

    public static Evidencia audio(Estudiante estudiante, Nivel nivel, String audioUrl) {
        Evidencia evidencia = new Evidencia();
        evidencia.estudiante = estudiante;
        evidencia.nivel = nivel;
        evidencia.tipo = AUDIO;
        evidencia.audioUrl = audioUrl;
        return evidencia;
    }

    public static Evidencia texto(Estudiante estudiante, Nivel nivel, String texto) {
        Evidencia evidencia = new Evidencia();
        evidencia.estudiante = estudiante;
        evidencia.nivel = nivel;
        evidencia.tipo = TEXTO;
        evidencia.texto = texto;
        return evidencia;
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public Nivel getNivel() { return nivel; }
    public void setNivel(Nivel nivel) { this.nivel = nivel; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getResultadoIa() { return resultadoIa; }
    public void setResultadoIa(String resultadoIa) { this.resultadoIa = resultadoIa; }
    public String getEstadoRevision() { return estadoRevision; }
    public void setEstadoRevision(String estadoRevision) { this.estadoRevision = estadoRevision; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public void setRevisadoEn(LocalDateTime revisadoEn) { this.revisadoEn = revisadoEn; }

    public boolean estaPendiente() {
        return PENDIENTE.equals(estadoRevision);
    }
}
