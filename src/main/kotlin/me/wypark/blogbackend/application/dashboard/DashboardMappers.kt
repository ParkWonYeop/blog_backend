package me.wypark.blogbackend.application.dashboard

import me.wypark.blogbackend.domain.comment.Comment
import me.wypark.blogbackend.domain.post.Post
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

internal fun DashboardPostStatRow.toDashboardPostStat(zoneId: ZoneId) = DashboardPostStat(
    id = id,
    title = title,
    slug = slug,
    categoryName = categoryName,
    viewCount = viewCount,
    rangeViewCount = rangeViewCount,
    commentCount = commentCount,
    createdAt = createdAt.toOffsetDateTime(zoneId),
    updatedAt = updatedAt.toOffsetDateTimeOrNull(zoneId)
)

internal fun DashboardCategoryStatRow.toDashboardCategoryStat(zoneId: ZoneId) = DashboardCategoryStat(
    id = id,
    name = name,
    parentId = parentId,
    postCount = postCount,
    viewCount = viewCount,
    recentViewCount = recentViewCount,
    lastPublishedAt = lastPublishedAt.toOffsetDateTimeOrNull(zoneId),
    childrenCount = childrenCount
)

