package com.feedping.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedping.dto.request.RssSubscriptionByEmailRequest;
import com.feedping.dto.request.RssSubscriptionRequest;
import com.feedping.dto.request.RssUnsubscribeRequest;
import com.feedping.dto.response.RssSubscriptionPageResponse;
import com.feedping.dto.response.RssSubscriptionResponse;
import com.feedping.exception.ErrorCode;
import com.feedping.service.SubscriptionService;
import com.feedping.service.VerificationCodeStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(SubscriptionController.class)
@EnableSpringDataWebSupport
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private VerificationCodeStore verificationCodeStore;

    @Test
    @DisplayName("이메일 인증 후 RSS 피드 구독에 성공한다")
    void should_SubscribeRss_When_EmailVerifiedAndValidRequest() throws Exception {
        // given
        RssSubscriptionRequest request = new RssSubscriptionRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "rssUrl", "https://feedping.co.kr/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "FeedPing Blog");

        given(verificationCodeStore.isEmailVerified("test@example.com")).willReturn(true);
        willDoNothing().given(subscriptionService).subscribeRss(any(RssSubscriptionRequest.class));

        // when
        ResultActions result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));

        then(verificationCodeStore).should().isEmailVerified("test@example.com");
        then(subscriptionService).should().subscribeRss(any(RssSubscriptionRequest.class));
    }

    @Test
    @DisplayName("인증되지 않은 이메일로 구독 시도 시 예외가 발생한다")
    void should_ThrowException_When_EmailNotVerified() throws Exception {
        // given
        RssSubscriptionRequest request = new RssSubscriptionRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "rssUrl", "https://feedping.co.kr/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "FeedPing Blog");

        given(verificationCodeStore.isEmailVerified("test@example.com")).willReturn(false);

        // when
        ResultActions result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_NOT_VERIFIED.getMessage()));

        then(verificationCodeStore).should().isEmailVerified("test@example.com");
        then(subscriptionService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("토큰으로 구독 목록을 성공적으로 조회한다")
    void should_GetSubscriptions_When_ValidToken() throws Exception {
        // given
        String token = "valid-token";
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        RssSubscriptionResponse subscription = new RssSubscriptionResponse(1L, "https://feedping.co.kr/rss.xml",
                "FeedPing Blog");
        RssSubscriptionPageResponse response = new RssSubscriptionPageResponse(List.of(subscription), 1);

        given(subscriptionService.getSubscriptionsByEmail(eq(token), any(Pageable.class))).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/subscriptions/manage")
                .param("token", token));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.subscriptions[0].id").value(1))
                .andExpect(jsonPath("$.data.subscriptions[0].rssUrl").value("https://feedping.co.kr/rss.xml"))
                .andExpect(jsonPath("$.data.subscriptions[0].siteName").value("FeedPing Blog"))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        then(subscriptionService).should().getSubscriptionsByEmail(eq(token), any(Pageable.class));
    }

    @Test
    @DisplayName("토큰으로 RSS 피드 구독에 성공한다")
    void should_SubscribeRssWithToken_When_ValidRequest() throws Exception {
        // given
        RssSubscriptionByEmailRequest request = new RssSubscriptionByEmailRequest();
        ReflectionTestUtils.setField(request, "token", "valid-token");
        ReflectionTestUtils.setField(request, "rssUrl", "https://feedping.co.kr/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "FeedPing Blog");

        willDoNothing().given(subscriptionService).subscribeRssWithToken(any(RssSubscriptionByEmailRequest.class));

        // when
        ResultActions result = mockMvc.perform(post("/subscriptions/manage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));

        then(subscriptionService).should().subscribeRssWithToken(any(RssSubscriptionByEmailRequest.class));
    }

    @Test
    @DisplayName("토큰으로 RSS 피드 구독 해지에 성공한다")
    void should_UnsubscribeRssWithToken_When_ValidRequest() throws Exception {
        // given
        Long subId = 1L;
        RssUnsubscribeRequest request = new RssUnsubscribeRequest();
        ReflectionTestUtils.setField(request, "token", "valid-token");

        willDoNothing().given(subscriptionService).unsubscribeRssWithToken(eq(subId), any(RssUnsubscribeRequest.class));

        // when
        ResultActions result = mockMvc.perform(delete("/subscriptions/manage/{subId}", subId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        then(subscriptionService).should().unsubscribeRssWithToken(eq(subId), any(RssUnsubscribeRequest.class));
    }

}
