package com.campus.freshman.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="campus.workspace.demo-mode=true")
class WorkspaceDemoAccessTest {
    @Autowired WebApplicationContext context;
    @Test void demoOpensWorkspaceWithoutLoginOrToken() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/workspace-access")).andExpect(status().isOk())
            .andExpect(jsonPath("$.requiresLogin").value(false));
        for(String resource:new String[]{"bases","models","agents","runs"}) {
            mvc.perform(get("/api/workspace/"+resource)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/workspace/bases").header("Authorization","Bearer expired-session"))
            .andExpect(status().isOk());
    }
}
