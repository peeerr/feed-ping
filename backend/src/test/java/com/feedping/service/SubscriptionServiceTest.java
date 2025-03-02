package com.feedping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.Subscription;
import com.feedping.dto.request.RssSubscriptionByEmailRequest;
import com.feedping.dto.request.RssSubscriptionRequest;
import com.feedping.dto.request.RssUnsubscribeRequest;
import com.feedping.dto.response.RssSubscriptionPageResponse;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.MemberRepository;
import com.feedping.repository.RssFeedRepository;
import com.feedping.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RssFeedRepository rssFeedRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private RssValidationService rssValidationService;

    @Test
    @DisplayName("유효한 이메일과 RSS URL로 구독에 성공한다")
    void should_SubscribeRss_When_ValidRequest() {
        // given
        RssSubscriptionRequest request = new RssSubscriptionRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "rssUrl", "https://example.com/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "Test Blog");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        willDoNothing().given(rssValidationService).validateRssUrl(anyString());
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(rssFeedRepository.findByUrl("https://example.com/rss.xml")).willReturn(Optional.of(rssFeed));
        given(subscriptionRepository.existsByMemberAndRssFeed(member, rssFeed)).willReturn(false);
        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
            Subscription savedSubscription = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedSubscription, "id", 1L);
            ReflectionTestUtils.setField(savedSubscription, "createdAt", LocalDateTime.now());
            return savedSubscription;
        });

        // when
        subscriptionService.subscribeRss(request);

        // then
        then(rssValidationService).should().validateRssUrl("https://example.com/rss.xml");
        then(memberRepository).should().findByEmail("test@example.com");
        then(rssFeedRepository).should().findByUrl("https://example.com/rss.xml");
        then(subscriptionRepository).should().existsByMemberAndRssFeed(member, rssFeed);
        then(subscriptionRepository).should().save(any(Subscription.class));
    }

    @Test
    @DisplayName("이미 구독 중인 RSS 피드를 구독하려고 하면 예외가 발생한다")
    void should_ThrowException_When_AlreadySubscribedRss() {
        // given
        RssSubscriptionRequest request = new RssSubscriptionRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "rssUrl", "https://example.com/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "Test Blog");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        willDoNothing().given(rssValidationService).validateRssUrl(anyString());
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(rssFeedRepository.findByUrl("https://example.com/rss.xml")).willReturn(Optional.of(rssFeed));
        given(subscriptionRepository.existsByMemberAndRssFeed(member, rssFeed)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribeRss(request))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_SUBSCRIBED_RSS);

        then(rssValidationService).should().validateRssUrl("https://example.com/rss.xml");
        then(memberRepository).should().findByEmail("test@example.com");
        then(rssFeedRepository).should().findByUrl("https://example.com/rss.xml");
        then(subscriptionRepository).should().existsByMemberAndRssFeed(member, rssFeed);
        then(subscriptionRepository).should(never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("토큰으로 구독 목록을 성공적으로 조회한다")
    void should_GetSubscriptionsByEmail_When_ValidToken() {
        // given
        String token = "valid-token";
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        Subscription subscription = Subscription.builder()
                .id(1L)
                .member(member)
                .rssFeed(rssFeed)
                .siteName("Test Blog")
                .build();

        ReflectionTestUtils.setField(subscription, "createdAt", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> subscriptionPage = new PageImpl<>(List.of(subscription), pageable, 1);

        given(authTokenService.validateAndGetMember(token)).willReturn(member);
        given(subscriptionRepository.findByMember(member, pageable)).willReturn(subscriptionPage);

        // when
        RssSubscriptionPageResponse response = subscriptionService.getSubscriptionsByEmail(token, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getSubscriptions()).hasSize(1);
        assertThat(response.getSubscriptions().get(0).getId()).isEqualTo(1L);
        assertThat(response.getSubscriptions().get(0).getRssUrl()).isEqualTo("https://example.com/rss.xml");
        assertThat(response.getSubscriptions().get(0).getSiteName()).isEqualTo("Test Blog");

        then(authTokenService).should().validateAndGetMember(token);
        then(subscriptionRepository).should().findByMember(member, pageable);
    }

    @Test
    @DisplayName("토큰으로 RSS 피드 구독에 성공한다")
    void should_SubscribeRssWithToken_When_ValidRequest() {
        // given
        RssSubscriptionByEmailRequest request = new RssSubscriptionByEmailRequest();
        ReflectionTestUtils.setField(request, "token", "valid-token");
        ReflectionTestUtils.setField(request, "rssUrl", "https://example.com/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "Test Blog");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        willDoNothing().given(rssValidationService).validateRssUrl(anyString());
        given(authTokenService.validateAndGetMember("valid-token")).willReturn(member);
        given(rssFeedRepository.findByUrl("https://example.com/rss.xml")).willReturn(Optional.of(rssFeed));
        given(subscriptionRepository.existsByMemberAndRssFeed(member, rssFeed)).willReturn(false);
        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
            Subscription savedSubscription = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedSubscription, "id", 1L);
            ReflectionTestUtils.setField(savedSubscription, "createdAt", LocalDateTime.now());
            return savedSubscription;
        });

        // when
        subscriptionService.subscribeRssWithToken(request);

        // then
        then(rssValidationService).should().validateRssUrl("https://example.com/rss.xml");
        then(authTokenService).should().validateAndGetMember("valid-token");
        then(rssFeedRepository).should().findByUrl("https://example.com/rss.xml");
        then(subscriptionRepository).should().existsByMemberAndRssFeed(member, rssFeed);
        then(subscriptionRepository).should().save(any(Subscription.class));
    }

    @Test
    @DisplayName("RSS 피드가 존재하지 않을 때 새로운 RSS 피드를 생성하고 구독한다")
    void should_CreateNewRssFeedAndSubscribe_When_RssFeedNotFound() {
        // given
        RssSubscriptionRequest request = new RssSubscriptionRequest();
        ReflectionTestUtils.setField(request, "email", "test@example.com");
        ReflectionTestUtils.setField(request, "rssUrl", "https://example.com/rss.xml");
        ReflectionTestUtils.setField(request, "siteName", "Test Blog");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed newRssFeed = RssFeed.builder()
                .url("https://example.com/rss.xml")
                .build();

        RssFeed savedRssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        willDoNothing().given(rssValidationService).validateRssUrl(anyString());
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(rssFeedRepository.findByUrl("https://example.com/rss.xml")).willReturn(Optional.empty());
        given(rssFeedRepository.save(any(RssFeed.class))).willReturn(savedRssFeed);
        given(subscriptionRepository.existsByMemberAndRssFeed(member, savedRssFeed)).willReturn(false);
        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> {
            Subscription savedSubscription = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedSubscription, "id", 1L);
            ReflectionTestUtils.setField(savedSubscription, "createdAt", LocalDateTime.now());
            return savedSubscription;
        });

        // when
        subscriptionService.subscribeRss(request);

        // then
        then(rssValidationService).should().validateRssUrl("https://example.com/rss.xml");
        then(memberRepository).should().findByEmail("test@example.com");
        then(rssFeedRepository).should().findByUrl("https://example.com/rss.xml");
        then(rssFeedRepository).should().save(any(RssFeed.class));
        then(subscriptionRepository).should().existsByMemberAndRssFeed(member, savedRssFeed);
        then(subscriptionRepository).should().save(any(Subscription.class));
    }

    @Test
    @DisplayName("토큰으로 RSS 피드 구독 해지에 성공한다")
    void should_UnsubscribeRssWithToken_When_ValidRequest() {
        // given
        Long subId = 1L;
        RssUnsubscribeRequest request = new RssUnsubscribeRequest();
        ReflectionTestUtils.setField(request, "token", "valid-token");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        Subscription subscription = Subscription.builder()
                .id(subId)
                .member(member)
                .rssFeed(rssFeed)
                .siteName("Test Blog")
                .build();

        given(authTokenService.validateAndGetMember("valid-token")).willReturn(member);
        given(subscriptionRepository.findById(subId)).willReturn(Optional.of(subscription));
        willDoNothing().given(subscriptionRepository).delete(any(Subscription.class));

        // when
        subscriptionService.unsubscribeRssWithToken(subId, request);

        // then
        then(authTokenService).should().validateAndGetMember("valid-token");
        then(subscriptionRepository).should().findById(subId);
        then(subscriptionRepository).should().delete(subscription);
    }

    @Test
    @DisplayName("구독 정보를 찾을 수 없을 때 구독 해지 시 예외가 발생한다")
    void should_ThrowException_When_SubscriptionNotFound() {
        // given
        Long subId = 999L;
        RssUnsubscribeRequest request = new RssUnsubscribeRequest();
        ReflectionTestUtils.setField(request, "token", "valid-token");

        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        given(authTokenService.validateAndGetMember("valid-token")).willReturn(member);
        given(subscriptionRepository.findById(subId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribeRssWithToken(subId, request))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUBSCRIPTION_NOT_FOUND);

        then(authTokenService).should().validateAndGetMember("valid-token");
        then(subscriptionRepository).should().findById(subId);
        then(subscriptionRepository).should(never()).delete(any(Subscription.class));
    }

}
