package com.feedping.notification;

import com.feedping.domain.RssItem;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NotificationTask {

    private final String email;
    private final Long rssFeedId;
    private final String siteName;
    private final List<RssItem> items;
    private final int retryCount;

    // 재시도를 위한 복사 생성자
    public NotificationTask withIncrementedRetryCount() {
        return NotificationTask.builder()
                .email(this.email)
                .rssFeedId(this.rssFeedId)
                .siteName(this.siteName)
                .items(this.items)
                .retryCount(this.retryCount + 1)
                .build();
    }

}
