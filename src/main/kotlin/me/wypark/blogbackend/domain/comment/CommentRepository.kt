package me.wypark.blogbackend.domain.comment

import me.wypark.blogbackend.domain.post.Post
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [댓글 데이터 접근 계층]
 *
 * 댓글 엔티티의 영속성 관리를 담당하는 리포지토리입니다.
 * 계층형 댓글 구조(Root-Child)를 효율적으로 조회하고,
 * 게시글 생명주기에 따른 종속적인 데이터 정리(Cleanup) 기능을 제공합니다.
 */
interface CommentRepository : JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 최상위(Root) 댓글 목록을 작성순으로 조회합니다.
     *
     * [계층형 데이터 조회 전략]
     * 대댓글(Child)까지 모두 Eager Fetch로 가져올 경우 데이터 중복(Cartesian Product) 및 애플리케이션 메모리 부하가 발생할 수 있습니다.
     * 따라서 Root 댓글만 우선 조회하고, 하위 댓글 컬렉션은 지연 로딩(Lazy Loading) 발생 시
     * 엔티티에 설정된 @BatchSize를 통해 IN 쿼리로 묶어서 가져오는 방식으로 N+1 문제를 최적화합니다.
     */
    fun findAllByPostAndParentIsNullOrderByCreatedAtAsc(post: Post): List<Comment>

    /**
     * 게시글 삭제 시, 해당 게시글에 종속된 모든 댓글을 삭제합니다.
     *
     * [데이터 무결성 관리]
     * 게시글(Post)이 사라지면 댓글(Comment)은 고아 데이터(Orphaned Data)가 되므로
     * 스토리지 낭비를 막고 참조 무결성을 유지하기 위해 함께 정리되어야 합니다.
     */
    fun deleteAllByPost(post: Post)
}