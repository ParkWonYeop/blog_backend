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

internal fun Post.toDashboardPostSummary(zoneId: ZoneId) = DashboardPostSummary(
    id = requireNotNull(id) { "Persisted post must have an id" },
    title = title,
    slug = slug,
    categoryName = category?.name ?: "미분류",
    viewCount = viewCount,
    createdAt = createdAt.toOffsetDateTime(zoneId),
    tags = tags.map { it.tag.name }
)

internal fun Comment.toDashboardCommentSummary(zoneId: ZoneId) = AdminDashboardCommentSummary(
    id = requireNotNull(id) { "Persisted comment must have an id" },
    content = content,
    author = getAuthorName(),
    guestNickname = guestNickname,
    memberNickname = member?.nickname,
    postSlug = post.slug,
    postTitle = post.title,
    createdAt = createdAt.toOffsetDateTime(zoneId)
)

internal fun LocalDateTime.toOffsetDateTime(zoneId: ZoneId): OffsetDateTime {
    return atZone(zoneId).toOffsetDateTime()
}

internal fun LocalDateTime?.toOffsetDateTimeOrNull(zoneId: ZoneId): OffsetDateTime? {
    return this?.toOffsetDateTime(zoneId)
}
