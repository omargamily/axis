package com.axis.pay.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: UUID? = null,
    @Column(unique = true, nullable = false)
    val email: String,
    @Column(nullable = false)
    val hashedPassword: String,
    @Column(nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
    @Column(nullable = false)
    val updatedAt: Long = System.currentTimeMillis()
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(GrantedAuthority { "ROLE_USER" })
    }

    override fun getPassword(): String {
        return hashedPassword
    }

    override fun getUsername(): String {
        return email
    }

    /** Can be later implemented to use flags from the database when needed */
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
