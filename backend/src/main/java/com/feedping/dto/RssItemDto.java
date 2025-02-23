package com.feedping.dto;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RssItemDto {

    private String title;        // <title>
    private String link;         // <link>
    private String description;  // <description>
    private LocalDateTime publishedAt;  // <pubDate>

    public static RssItemDto from(SyndEntry entry) {
        return new RssItemDto(
                entry.getTitle(),
                entry.getLink(),
                Optional.ofNullable(entry.getDescription())
                        .map(SyndContent::getValue)
                        .orElse(""),
                Optional.ofNullable(entry.getPublishedDate())
                        .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                        .orElse(LocalDateTime.now())
        );
    }

}
