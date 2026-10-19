package com.campus.freshman.rag;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.regex.Pattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="campus.workspace.demo-mode=false")
class WorkspaceHttpTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).build();}
    @Test void workspaceRequiresRealSessionAndSerializesFields() throws Exception {
        mvc.perform(get("/api/workspace-access")).andExpect(status().isOk()).andExpect(jsonPath("$.requiresLogin").value(true));
        mvc.perform(get("/api/workspace/bases")).andExpect(status().isUnauthorized());
        String json=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher=Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(json);assertThat(matcher.find()).isTrue();String token=matcher.group(1);
        mvc.perform(post("/api/workspace/bases").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"HTTP验收知识库\",\"chunkSize\":800,\"overlap\":100,\"chunkMode\":\"GENERAL\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("HTTP验收知识库")).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(get("/api/workspace/bases").header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }
}
