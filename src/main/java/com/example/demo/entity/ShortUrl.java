    package com.example.demo.entity;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;

    @Data
    @Entity
    @Table(name = "short_url")
    @AllArgsConstructor @NoArgsConstructor @Builder
    public class ShortUrl {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "short_key", length = 10, nullable = false, unique = true)
        private String shortKey;

        // TEXT存磁盘 可以更长 但不能 unique 没用VARCHAR存内存快
        @Column(name = "long_url", length = 500, nullable = false, unique = true)
        private String longUrl;

        @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
        private LocalDateTime updatedAt;

        @Column(name = "access_count", nullable = false, columnDefinition = "INT DEFAULT 0")
        private Integer accessCount;


        @PrePersist
        protected void onCreate() {
            if (this.createdAt == null) {
                this.createdAt = LocalDateTime.now();
            }
            if (this.accessCount == null) {
                this.accessCount = 0;
            }
        }
        @PreUpdate
        protected void onUpdate() {
            this.updatedAt = LocalDateTime.now();
        }


    }
