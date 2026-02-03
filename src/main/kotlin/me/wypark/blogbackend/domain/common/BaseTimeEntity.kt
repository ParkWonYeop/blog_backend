package me.wypark.blogbackend.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * [공통 시간 정보 엔티티]
 *
 * 모든 엔티티가 공통적으로 가져야 할 '생성 시간'과 '수정 시간'을 관리하는 상위 클래스입니다.
 *
 * [설계 의도]
 * 반복적인 감사(Audit) 로직을 중복 구현하는 것을 방지하기 위해 JPA Auditing 기능을 적용했습니다.
 * 이를 상속받는 엔티티들은 별도의 코드 작성 없이 데이터의 생명주기를 자동으로 추적할 수 있습니다.
 */
@MappedSuperclass // 테이블로 매핑되지 않고, 자식 클래스의 엔티티에 컬럼 정보만 제공함 (상속 관계 매핑 X)
@EntityListeners(AuditingEntityListener::class) // 엔티티의 변경 이벤트를 감지하여 시간 값을 자동으로 주입(Inject)
abstract class BaseTimeEntity {

    /**
     * 최초 생성 시각 (Immutable)
     * 데이터의 이력을 추적하는 기준이 되므로, 생성 이후에는 절대 변경되지 않도록 updatable = false를 설정하여 무결성을 보장합니다.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    /**
     * 최종 수정 시각
     * 비즈니스 로직에 의해 데이터가 변경될 때마다 JPA가 자동으로 현재 시간을 갱신합니다.
     */
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}