package com.feedping.repository;

import com.feedping.domain.RssFeed;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RssFeedRepository extends JpaRepository<RssFeed, Long> {

    Optional<RssFeed> findByUrl(String url);

}
