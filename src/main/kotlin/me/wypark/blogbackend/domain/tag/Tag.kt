package me.wypark.blogbackend.domain.tag

import jakarta.persistence.*

@Entity
@Table(name = "tag")
class Tag(
    @Column(nullable = false, unique = true)
    val name: String
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}