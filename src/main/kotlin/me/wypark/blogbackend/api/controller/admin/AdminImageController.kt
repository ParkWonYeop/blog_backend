package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.domain.image.ImageService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * [관리자용 이미지 업로드 API]
 *
 * 게시글 본문(Markdown) 삽입용 이미지나 프로필 사진 등, 블로그 운영에 필요한
 * 정적 리소스(Static Resources)를 처리하는 컨트롤러입니다.
 *
 * 스토리지 저장소(S3/MinIO)와의 직접적인 통신은 Service Layer에 위임하며,
 * 클라이언트에게는 업로드된 리소스의 접근 가능한 URL을 반환하여 즉시 렌더링 가능하도록 합니다.
 */
@RestController
@RequestMapping("/api/admin/images")
class AdminImageController(
    private val imageService: ImageService
) {

    /**
     * 이미지를 업로드하고 접근 가능한 URL을 반환합니다.
     *
     * 주로 에디터(Toast UI 등)에서 이미지 첨부 이벤트가 발생했을 때 비동기로 호출되며,
     * 업로드 성공 시 반환된 URL은 클라이언트 측에서 즉시 Markdown 문법(![alt](url))으로 변환되어 본문에 삽입됩니다.
     *
     * @param image 클라이언트가 전송한 바이너리 파일 (MultipartFile)
     * @return CDN 또는 스토리지의 접근 가능한 절대 경로 (URL)
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<String>> {
        val imageUrl = imageService.uploadImage(image)
        return ResponseEntity.ok(ApiResponse.success(imageUrl, "이미지 업로드 성공"))
    }
}