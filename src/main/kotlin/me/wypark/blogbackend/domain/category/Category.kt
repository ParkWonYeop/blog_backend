package me.wypark.blogbackend.domain.category

import jakarta.persistence.*

/**
 * [카테고리 엔티티]
 *
 * 게시글 분류를 위한 계층형 구조(Hierarchical Structure)를 정의합니다.
 * 자기 자신을 참조하는 Self-Referencing 방식을 사용하여 무한 깊이의 트리 구조를 구현했습니다.
 */
@Entity
class Category(
    @Column(nullable = false)
    var name: String,

    /**
     * [부모 카테고리]
     * 루트(Root) 카테고리의 경우 null을 허용합니다.
     * N+1 문제를 방지하기 위해 기본 Fetch 전략을 LAZY로 설정했습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    /**
     * [자식 카테고리 목록]
     *
     * [Cascade 설정]
     * 부모 카테고리가 삭제될 경우, 데이터 무결성을 위해 하위 카테고리들도 함께 삭제(CascadeType.ALL)되도록 설정했습니다.
     * (실무 정책에 따라 삭제 대신 '미분류'로 이동시키거나 삭제를 막을 수도 있습니다.)
     */
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<Category> = mutableListOf()
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    /**
     * [연관관계 편의 메서드]
     *
     * 양방향 관계인 Category 엔티티에서 부모와 자식 간의 참조를 원자적(Atomic)으로 설정합니다.
     * 객체 관점에서 부모의 children 리스트에도 추가하고, 자식의 parent 필드도 설정해주어야
     * 영속성 컨텍스트(Persistence Context) 내에서 데이터 정합성이 유지됩니다.
     */
    fun addChild(child: Category) {
        this.children.add(child)
        child.parent = this
    }

    fun updateName(name: String) {
        this.name = name
    }

    /**
     * [부모 카테고리 변경 (이동)]
     *
     * 카테고리의 위치를 트리 구조 내에서 이동시킵니다.
     * 기존 부모와의 관계를 명시적으로 끊고 새로운 부모와 연결함으로써,
     * JPA 1차 캐시 상의 데이터 불일치를 방지합니다.
     */
    fun changeParent(newParent: Category?) {
        // 1. 기존 부모와의 관계 끊기 (메모리 상의 리스트 정리)
        this.parent?.children?.remove(this)

        // 2. 새 부모 설정
        this.parent = newParent

        // 3. 새 부모의 자식 목록에 추가 (null이 아니면)
        newParent?.children?.add(this)
    }
}