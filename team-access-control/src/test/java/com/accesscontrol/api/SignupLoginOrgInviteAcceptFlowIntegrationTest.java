package com.accesscontrol.api;

import com.accesscontrol.api.dto.*;
import com.accesscontrol.api.service.EmailService;
import com.accesscontrol.api.service.RedisSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SignupLoginOrgInviteAcceptFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tacapi_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Flyway runs migrations against this real, throwaway container automatically.
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Real email sending is mocked out — we don't want tests hitting Gmail SMTP.
    @MockBean
    private EmailService emailService;

    // Redis isn't spun up in this test — mock it out so session storage doesn't
    // fail at runtime. Acceptable for this test's purpose (verifying HTTP flow +
    // real DB state), not testing Redis itself.
    @MockBean
    private RedisSessionService redisSessionService;

    @Test
    void fullSignupLoginOrgInviteAcceptFlow() throws Exception {
        doNothing().when(redisSessionService).storeActiveSession(any(), anyLong());
        doNothing().when(emailService).sendInvitationEmail(anyString(), anyString(), anyString());

        // --- 1. Owner signs up ---
        SignupRequest ownerSignup = new SignupRequest();
        ownerSignup.setEmail("integration-owner@test.com");
        ownerSignup.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ownerSignup)))
                .andExpect(status().isOk());

        // --- 2. Owner logs in ---
        LoginRequest ownerLogin = new LoginRequest();
        ownerLogin.setEmail("integration-owner@test.com");
        ownerLogin.setPassword("password123");

        String ownerLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ownerLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String ownerAccessToken = objectMapper.readTree(ownerLoginResponse).get("accessToken").asText();
        assertThat(ownerAccessToken).isNotBlank();

        // --- 3. Owner creates an organization ---
        CreateOrganizationRequest createOrg = new CreateOrganizationRequest();
        createOrg.setName("Integration Test Org");
        createOrg.setSlug("integration-test-org");

        String orgResponse = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createOrg)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String orgId = objectMapper.readTree(orgResponse).get("id").asText();
        assertThat(orgId).isNotBlank();

        // --- 4. Invitee signs up separately (already-registered-user flow) ---
        SignupRequest inviteeSignup = new SignupRequest();
        inviteeSignup.setEmail("integration-invitee@test.com");
        inviteeSignup.setPassword("password456");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteeSignup)))
                .andExpect(status().isOk());

        // --- 5. Owner invites that email as "member" ---
        InviteRequest invite = new InviteRequest();
        invite.setEmail("integration-invitee@test.com");
        invite.setRoleName("member");

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invite")
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invite)))
                .andExpect(status().isOk());

        // Capture the raw invite token from the mocked EmailService call —
        // this is how the test gets the token without a real inbox.
        org.mockito.ArgumentCaptor<String> tokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendInvitationEmail(eq("integration-invitee@test.com"), anyString(), tokenCaptor.capture());
        String rawInviteToken = tokenCaptor.getValue();
        assertThat(rawInviteToken).isNotBlank();

        // --- 6. Invitee logs in ---
        LoginRequest inviteeLogin = new LoginRequest();
        inviteeLogin.setEmail("integration-invitee@test.com");
        inviteeLogin.setPassword("password456");

        String inviteeLoginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inviteeLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String inviteeAccessToken = objectMapper.readTree(inviteeLoginResponse).get("accessToken").asText();

        // --- 7. Invitee accepts the invitation ---
        mockMvc.perform(post("/api/v1/organizations/accept-invite")
                        .header("Authorization", "Bearer " + inviteeAccessToken)
                        .param("token", rawInviteToken))
                .andExpect(status().isOk());

        // --- 8. Invitee's org list now really contains this org, with role "member" ---
        String inviteeOrgsResponse = mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", "Bearer " + inviteeAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(inviteeOrgsResponse).contains("Integration Test Org");
        assertThat(inviteeOrgsResponse).contains("\"roleName\":\"member\"");
    }
}