package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.model.Avatar;
import com.proyecto.usuario.docente.repository.AvatarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-185: el listado de avatares sale ordenado y con id, nombre y url
class AvatarControllerTest {

    private AvatarRepository repositorio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repositorio = mock(AvatarRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new AvatarController(repositorio)).build();
    }

    private Avatar avatar(int id, String nombre, String url) {
        Avatar a = mock(Avatar.class);
        when(a.getId()).thenReturn((short) id);
        when(a.getNombre()).thenReturn(nombre);
        when(a.getUrl()).thenReturn(url);
        return a;
    }

    @Test
    void listar_devuelveLosAvataresOrdenadosPorId() throws Exception {
        // Primero se crean los avatares; si se crean dentro de thenReturn, Mockito falla
        Avatar zorro = avatar(1, "Zorro", "https://img/zorro.png");
        Avatar buho = avatar(2, "Búho", "https://img/buho.png");
        when(repositorio.findAll(Sort.by("id"))).thenReturn(List.of(zorro, buho));

        mvc.perform(get("/api/docente/avatares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Zorro"))
                .andExpect(jsonPath("$[1].url").value("https://img/buho.png"));

        verify(repositorio).findAll(Sort.by("id")); // pidió el orden por id
    }

    @Test
    void listar_sinAvatares_devuelveListaVacia() throws Exception {
        when(repositorio.findAll(Sort.by("id"))).thenReturn(List.of());

        mvc.perform(get("/api/docente/avatares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}