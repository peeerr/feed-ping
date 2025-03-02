package com.feedping.controller;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedping.dto.request.EmailRevokeRequest;
import com.feedping.dto.request.EmailVerificationConfirmRequest;
import com.feedping.dto.request.EmailVerificationSendRequest;
import com.feedping.service.EmailVerificationService;
import com.feedping.util.EmailVerificationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(EmailVerificationController.class)
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private EmailVerificationManager emailVerificationManager;

    @Test
    @DisplayName("이메일 인증 코드가 성공적으로 전송된다")
    void should_SendVerificationEmail_When_ValidRequest() throws Exception {
        // given
        EmailVerificationSendRequest request = new EmailVerificationSendRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");

        willDoNothing().given(emailVerificationService).sendVerificationEmail(anyString());

        // when
        ResultActions result = mockMvc.perform(post("/email-verification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.async").value(true))
                .andExpect(jsonPath("$.message").exists());

        then(emailVerificationService).should().sendVerificationEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일 인증이 성공적으로 완료된다")
    void should_VerifyEmail_When_ValidCode() throws Exception {
        // given
        EmailVerificationConfirmRequest request = new EmailVerificationConfirmRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "code", "123456");

        willDoNothing().given(emailVerificationService).verifyEmail(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(post("/email-verification/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        then(emailVerificationService).should().verifyEmail("test@example.com", "123456");
    }

    @Test
    @DisplayName("이메일 인증 상태가 성공적으로 확인된다")
    void should_CheckAuthStatus_When_ValidEmail() throws Exception {
        // given
        String email = "test@example.com";
        given(emailVerificationManager.isEmailVerified(email)).willReturn(true);

        // when
        ResultActions result = mockMvc.perform(get("/email-verification/status")
                .param("email", email));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        then(emailVerificationManager).should().isEmailVerified(email);
    }

    @Test
    @DisplayName("이메일 인증 정보가 성공적으로 취소된다")
    void should_RevokeVerification_When_ValidRequest() throws Exception {
        // given
        EmailRevokeRequest request = new EmailRevokeRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");

        willDoNothing().given(emailVerificationManager).revokeVerification(anyString());

        // when
        ResultActions result = mockMvc.perform(post("/email-verification/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        then(emailVerificationManager).should().revokeVerification("test@example.com");
    }

}
