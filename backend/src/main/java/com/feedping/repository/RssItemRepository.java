package com.feedping.repository;

import com.feedping.domain.RssItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RssItemRepository extends JpaRepository<RssItem, Long> {

    Optional<RssItem> findByLink(String link);

}
