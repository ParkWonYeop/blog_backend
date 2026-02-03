package me.wypark.blogbackend.api.controller.admin

import jakarta.validation.Valid
import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.api.dto.PostSaveRequest
import me.wypark.blogbackend.domain.post.PostService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*

/**
 * [관리자용 게시글 관리 API]
 *
 * 블로그 콘텐츠(Post)의 전체 수명 주기(Lifecycle)를 관리하는 컨트롤러입니다.
 * 게시글의 작성, 수정, 삭제 기능을 제공하며, 이 과정에서 입력값 검증(@Valid)과
 * 데이터 무결성 유지를 위한 다양한 비즈니스 로직(Slug 생성, 태그 처리 등)을 조율합니다.
 */
@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val postService: PostService
) {

    /**
     * 신규 게시글을 작성 및 발행합니다.
     *
     * Security Context에서 현재 로그인한 관리자 정보를 추출하여 작성자(Author)로 매핑함으로써,
     * 클라이언트가 임의로 작성자를 변조하는 것을 방지합니다.
     */
    @PostMapping
    fun createPost(
        @RequestBody @Valid request: PostSaveRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<Long>> {
        val postId = postService.createPost(request, user.username)
        return ResponseEntity.ok(ApiResponse.success(postId, "게시글이 작성되었습니다."))
    }

    /**
     * 기존 게시글을 수정합니다.
     *
     * 제목이나 본문 변경뿐만 아니라, 카테고리 이동이나 태그 재설정과 같은 메타데이터 변경도 함께 처리합니다.
     * 수정 시 사용되지 않게 된 이미지를 정리하거나, URL(Slug) 변경에 따른 리다이렉트 고려 등이 서비스 레이어에서 처리됩니다.
     */
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @RequestBody @Valid request: PostSaveRequest
    ): ResponseEntity<ApiResponse<Long>> {
        val postId = postService.updatePost(id, request)
        return ResponseEntity.ok(ApiResponse.success(postId, "게시글이 수정되었습니다."))
    }

    /**
     * 게시글을 영구 삭제합니다.
     *
     * [리소스 정리 전략]
     * 단순히 DB 레코드(Row)만 삭제하는 것이 아니라, 해당 게시글 본문에 포함되었던
     * S3 업로드 이미지 파일들을 추적하여 함께 삭제(Cleanup)합니다.
     * 이를 통해 스토리지에 불필요한 고아 파일(Orphaned Files)이 누적되는 것을 방지하여 비용을 최적화합니다.
     */
    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing>> {
        postService.deletePost(id)
        return ResponseEntity.ok(ApiResponse.success(message = "게시글과 포함된 이미지가 삭제되었습니다."))
    }
}