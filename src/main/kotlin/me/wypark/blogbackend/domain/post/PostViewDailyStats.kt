package me.wypark.blogbackend.domain.post

import jakarta.persistence.*
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import java.time.LocalDate

@Entity
@Table(
    name = "post_view_daily_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_view_daily_stats_post_date",
            columnNames = ["post_id", "stat_date"]
        )
    ],
    indexes = [
        Index(name = "idx_post_view_daily_stats_date", columnList = "stat_date"),
        Index(name = "idx_post_view_daily_stats_post_id", columnList = "post_id")
    ]
)
class PostViewDailyStats(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(name = "stat_date", nullable = false)
    val statDate: LocalDate,

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
