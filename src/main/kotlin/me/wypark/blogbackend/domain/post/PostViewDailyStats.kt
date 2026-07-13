package me.wypark.blogbackend.domain.post

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
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

    viewCount: Long = 0
) : BaseTimeEntity() {

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
