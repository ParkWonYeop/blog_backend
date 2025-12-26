package me.wypark.blogbackend.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass // 상속받은 엔티티가 이 클래스의 필드(컬럼)를 인식하도록 함
@EntityListeners(AuditingEntityListener::class) // JPA Auditing 기능 활성화
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false) // 생성일은 수정 불가
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}