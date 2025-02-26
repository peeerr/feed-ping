package com.feedping.event;

import com.feedping.domain.Member;
import com.feedping.domain.RssItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * RSS 알림 발송 이벤트
 * 트랜잭션 완료 후 이메일 발송을 처리하기 위한 이벤트
 */
@Getter
@AllArgsConstructor
public class RssNotificationEvent {

    private final Map<Member, List<RssItem>> notificationMap;
    private final String siteName;

}
