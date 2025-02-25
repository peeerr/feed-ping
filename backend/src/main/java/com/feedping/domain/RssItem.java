package com.feedping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class RssItem {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private RssFeed rssFeed;

    @Column(nullable = false, unique = true, length = 4096)
    private String link;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4096)
    private String description;

    private LocalDateTime publishedAt;

    @Builder
    public RssItem(Long id, RssFeed rssFeed, String link, String title, String description, LocalDateTime publishedAt) {
        this.id = id;
        this.rssFeed = rssFeed;
        this.link = link;
        this.title = title;
        this.description = description;
        this.publishedAt = publishedAt;
    }

}
