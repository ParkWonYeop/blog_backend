package me.wypark.blogbackend.domain.category

import jakarta.persistence.*

@Entity
class Category(
    @Column(nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null, // 부모 카테고리 (없으면 최상위)

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<Category> = mutableListOf() // 자식 카테고리들
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    // 연관관계 편의 메서드 (부모-자식 연결)
    fun addChild(child: Category) {
        this.children.add(child)
        child.parent = this
    }

    // 이름 변경
    fun updateName(name: String) {
        this.name = name
    }

    // 부모 변경 (계층 이동)
    fun changeParent(newParent: Category?) {
        // 1. 기존 부모와의 관계 끊기
        this.parent?.children?.remove(this)

        // 2. 새 부모 설정
        this.parent = newParent

        // 3. 새 부모의 자식 목록에 추가 (null이 아니면)
        newParent?.children?.add(this)
    }
}