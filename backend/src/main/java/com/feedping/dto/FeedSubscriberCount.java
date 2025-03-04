package com.feedping.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FeedSubscriberCount {

    private Long rssFeedId;
    private Long subscriberCount;

    public int getSubscriberCountAsInt() {
        return subscriberCount != null ? subscriberCount.intValue() : 0;
    }

}
