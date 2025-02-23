package com.feedping.repository;

import com.feedping.domain.RssItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RssItemRepository extends JpaRepository<RssItem, Long> {

    boolean existsByLink(String link);

}
