package com.musicstore.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le proxy de « ng serve » transmet l'origine du navigateur : la configuration CORS doit
 * accepter le serveur de développement quel que soit le port sur lequel il a été démarré.
 */
class CorsConfigIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("le prevol est accepte depuis n importe quel port de localhost")
    void accepteToutPortLocal() throws Exception {
        for (String origine : new String[] {"http://localhost:4200", "http://localhost:4300", "http://localhost:8081"}) {
            mockMvc.perform(options("/api/titres")
                            .header(HttpHeaders.ORIGIN, origine)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origine));
        }
    }

    @Test
    @DisplayName("les en-tetes de plage sont exposes au navigateur, sans quoi le seek est impossible")
    void exposeLesEnTetesDePlage() throws Exception {
        mockMvc.perform(options("/api/media/00000000-0000-0000-0000-000000000000")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.containsString("Content-Range")));
    }

    @Test
    @DisplayName("une origine etrangere est refusee")
    void refuseUneOrigineEtrangere() throws Exception {
        mockMvc.perform(get("/api/titres").header(HttpHeaders.ORIGIN, "https://site-tiers.example"))
                .andExpect(status().isForbidden());
    }
}
