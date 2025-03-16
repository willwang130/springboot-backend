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

    @Modifying
    @Query("UPDATE ShortUrl u SET u.accessCount = u.accessCount + :count WHERE u.shortKey = :shortKey")
    void incrementAccessCount(@Param("shortKey") String shortKey, @Param("count") int count);

    // 查询最近 N 分钟内access_count更新,访问 MySQL 总次数
    @Query(value = "SELECT SUM(access_count) FROM short_url WHERE updated_at >= NOW() - INTERVAL ?1 MINUTE", nativeQuery = true)
    Optional<Long> sumUpdatesLastNMin(@Param("minutes") int minutes);

    @Modifying
    @Query("UPDATE ShortUrl u SET u.accessCount = 0")
    void resetAllAccessCounts();

}
