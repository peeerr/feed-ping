package com.feedping.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feedping.domain.Member;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.service.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TokenController.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("유효한 토큰 검증에 성공한다")
    void should_ValidateToken_When_TokenIsValid() throws Exception {
        // given
        String token = "valid-token";
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        given(authTokenService.validateAndGetMember(token)).willReturn(member);

        // when
        ResultActions result = mockMvc.perform(get("/token/validate")
                .param("token", token));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        then(authTokenService).should().validateAndGetMember(token);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 시 예외가 발생한다")
    void should_ThrowException_When_TokenIsInvalid() throws Exception {
        // given
        String token = "invalid-token";

        given(authTokenService.validateAndGetMember(token))
                .willThrow(new GlobalException(ErrorCode.INVALID_TOKEN));

        // when
        ResultActions result = mockMvc.perform(get("/token/validate")
                .param("token", token));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));

        then(authTokenService).should().validateAndGetMember(token);
    }

    @Test
    @DisplayName("토큰 파라미터가 없을 때 BadRequest 상태가 반환된다")
    void should_ReturnBadRequest_When_TokenParameterIsMissing() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/token/validate"));

        // then
        result.andExpect(status().isBadRequest());
    }

}
