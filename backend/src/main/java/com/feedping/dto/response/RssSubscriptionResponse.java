package com.feedping.dto.response;

import com.feedping.domain.Subscription;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RssSubscriptionResponse {

    private Long id;
    private String rssUrl;
    private String siteName;

    public static RssSubscriptionResponse from(Subscription subscription) {
        Long id = subscription.getId();
        String rssUrl = subscription.getRssFeed().getUrl();
        String siteName = subscription.getSiteName();

        return new RssSubscriptionResponse(id, rssUrl, siteName);
    }

}
