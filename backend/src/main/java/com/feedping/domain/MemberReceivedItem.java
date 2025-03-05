package com.feedping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "rss_item_id"}, name = "uk_member_rss_item")
})
@Entity
public class MemberReceivedItem {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private RssItem rssItem;

    @Column(nullable = false)
    private boolean notified = false;

    @Builder
    public MemberReceivedItem(Member member, RssItem rssItem) {
        this.member = member;
        this.rssItem = rssItem;
    }

    public void markAsNotified() {
        this.notified = true;
    }

}
