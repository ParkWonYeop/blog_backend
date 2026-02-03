package me.wypark.blogbackend.domain.post

import jakarta.persistence.*
import me.wypark.blogbackend.domain.category.Category
import me.wypark.blogbackend.domain.common.BaseTimeEntity
import me.wypark.blogbackend.domain.tag.PostTag
import me.wypark.blogbackend.domain.user.Member

/**
 * [게시글 엔티티]
 *
 * 블로그의 핵심 콘텐츠인 게시글(Post) 데이터를 정의하는 도메인 모델입니다.
 *
 * [설계 의도]
 * - Setter 사용을 지양하고, 비즈니스 의미가 명확한 편의 메서드(update, addTags 등)를 통해 상태를 변경하도록 설계하여
 * 객체의 일관성(Consistency)과 코드의 응집도(Cohesion)를 높였습니다.
 * - 조회수(viewCount)와 같은 동시성 처리가 필요한 필드는 별도의 증가 메서드로 관리합니다.
 */
@Entity
class Post(
    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false, unique = true)
    var slug: String,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    /**
     * [태그 매핑 전략]
     * PostTag 엔티티와의 일대다 관계를 통해 태그 정보를 관리합니다.
     * 게시글이 삭제되거나 수정될 때 태그 연결 정보도 함께 정리되어야 하므로
     * CascadeType.ALL과 orphanRemoval=true 옵션을 사용하여 생명주기를 동기화했습니다.
     */
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tags: MutableList<PostTag> = mutableListOf()
) : BaseTimeEntity() {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * 조회수를 1 증가시킵니다.
     * Note: 높은 트래픽 환경에서는 DB Lock 경합이 발생할 수 있으므로,
     * Redis HyperLogLog 등을 활용한 캐싱 후 배치 업데이트(Write-Back) 전략을 고려할 수 있습니다.
     */
    fun increaseViewCount() {
        this.viewCount++
    }

    fun addTags(postTags: List<PostTag>) {
        this.tags.addAll(postTags)
    }

    /**
     * [게시글 수정 편의 메서드]
     *
     * 제목, 본문, 슬러그, 카테고리 등 주요 필드를 한 번에 업데이트합니다.
     * JPA의 변경 감지(Dirty Checking) 기능에 의해 트랜잭션 종료 시점에 자동으로 Update 쿼리가 실행됩니다.
     */
    fun update(title: String, content: String, slug: String, category: Category?) {
        this.title = title
        this.content = content
        this.slug = slug
        this.category = category
    }

    /**
     * [태그 전체 교체 로직]
     *
     * 기존 태그 목록을 모두 비우고(clear) 새로운 태그들로 대체합니다.
     * orphanRemoval = true 설정에 의해, 컬렉션에서 제거된 기존 PostTag 엔티티들은
     * DB에서도 자동으로 삭제(DELETE) 처리됩니다.
     */
    fun updateTags(newTags: List<PostTag>) {
        this.tags.clear()
        this.tags.addAll(newTags)
    }
}