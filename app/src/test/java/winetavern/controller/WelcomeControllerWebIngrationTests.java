package winetavern.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import winetavern.AbstractWebIntegrationTests;
import org.junit.Test;


/**
 * Web integration tests for the {@link WelcomeController}
 * @author Niklas Wünsche
 */

public class WelcomeControllerWebIngrationTests extends AbstractWebIntegrationTests {

    @Test
    public void redirectsIfAdmin() throws Exception {
        mvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(authenticated())
                .andExpect(status().isOk())
                .andExpect(view().name("backend-temp"));
    }

    @Test
    public void redirectToUsers() throws Exception {
        mvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(authenticated())
                .andExpect(status().isOk())
                .andExpect(view().name("users"));
    }

}
