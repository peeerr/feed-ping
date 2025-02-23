package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.Subscription;
import com.feedping.dto.request.RssSubscriptionByEmailRequest;
import com.feedping.dto.request.RssSubscriptionRequest;
import com.feedping.dto.request.RssUnsubscribeRequest;
import com.feedping.dto.response.RssSubscriptionPageResponse;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.AuthTokenRepository;
import com.feedping.repository.MemberRepository;
import com.feedping.repository.RssFeedRepository;
import com.feedping.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final RssFeedRepository rssFeedRepository;
    private final AuthTokenRepository tokenRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void subscribeRss(RssSubscriptionRequest request) {
        String email = request.getEmail();
        String rssUrl = request.getRssUrl();
        String siteName = request.getSiteName();
        Member member = getMemberByEmail(email);

        subscribe(member, rssUrl, siteName);
    }

    @Transactional(readOnly = true)
    public RssSubscriptionPageResponse getSubscriptionsByEmail(String token, Pageable pageable) {
        Member member = getMemberByToken(token);
        Page<Subscription> subscriptions = subscriptionRepository.findByMember(member, pageable);
        return RssSubscriptionPageResponse.of(subscriptions);
    }

    public void subscribeRssWithToken(RssSubscriptionByEmailRequest request) {
        String token = request.getToken();
        String rssUrl = request.getRssUrl();
        String siteName = request.getSiteName();
        Member member = getMemberByToken(token);

        subscribe(member, rssUrl, siteName);
    }

    public void unsubscribeRssWithToken(Long rssId, RssUnsubscribeRequest request) {
        Member member = getMemberByToken(request.getToken());
        RssFeed rssFeed = getRssFeed(rssId);

        Subscription subscription = subscriptionRepository.findByMemberAndRssFeed(member, rssFeed)
                .orElseThrow(() -> new GlobalException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscriptionRepository.delete(subscription);
    }

    private void subscribe(Member member, String rssUrl, String siteName) {
        // RSS Feed가 DB에 없으면 저장
        RssFeed rssFeed = rssFeedRepository.findByUrl(rssUrl)
                .orElseGet(() -> rssFeedRepository.save(
                        RssFeed.builder()
                                .url(rssUrl)
                                .build()
                ));

        // 이미 구독 중인지 확인
        if (subscriptionRepository.existsByMemberAndRssFeed(member, rssFeed)) {
            throw new GlobalException(ErrorCode.ALREADY_SUBSCRIBED_RSS);
        }

        // 새로운 구독 저장
        Subscription subscription = Subscription.builder()
                .member(member)
                .rssFeed(rssFeed)
                .siteName(siteName)
                .build();
        subscriptionRepository.save(subscription);
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND_MEMBER));
    }

    private Member getMemberByToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_TOKEN))
                .getMember();
    }

    private RssFeed getRssFeed(Long rssId) {
        return rssFeedRepository.findById(rssId)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND_RSS_FEED));
    }

}
