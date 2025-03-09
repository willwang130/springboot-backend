package com.example.demo.repository;

import com.example.demo.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortKey(String shortKey);

    ShortUrl findByLongUrl(String longUrl);

    List<ShortUrl> findByAccessCountGreaterThan(int i);

    @Transactional
    @Modifying
    @Query("UPDATE ShortUrl u SET u.accessCount = u.accessCount + :count WHERE u.shortKey = :shortKey")
    void incrementAccessCount(@Param("shortKey") String shortKey, @Param("count") int count);
}
