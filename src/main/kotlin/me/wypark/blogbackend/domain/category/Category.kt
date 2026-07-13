package me.wypark.blogbackend.domain.category

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
class Category(
    name: String,
    parent: Category? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<Category> = mutableListOf()
) {
    @Column(nullable = false)
    var name: String = name
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = parent
        protected set

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun addChild(child: Category) {
        this.children.add(child)
        child.parent = this
    }

    fun updateName(name: String) {
        this.name = name
    }

    fun changeParent(newParent: Category?) {
        this.parent?.children?.remove(this)

        this.parent = newParent

        newParent?.children?.add(this)
    }
}
