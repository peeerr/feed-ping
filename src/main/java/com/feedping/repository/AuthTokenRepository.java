package com.feedping.repository;

import com.feedping.domain.AuthToken;
import com.feedping.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByToken(String token);

    boolean existsByMember(Member member);

}
