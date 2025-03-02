package com.feedping.dto.response;

import com.feedping.domain.Subscription;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@AllArgsConstructor
@Getter
public class RssSubscriptionPageResponse {

    private List<RssSubscriptionResponse> subscriptions;
    private int totalCount;

    public static RssSubscriptionPageResponse of(Page<Subscription> subscriptionPage) {
        List<RssSubscriptionResponse> responses = subscriptionPage.getContent()
                .stream()
                .map(RssSubscriptionResponse::from)
                .toList();

        return new RssSubscriptionPageResponse(responses, (int) subscriptionPage.getTotalElements());
    }

}
