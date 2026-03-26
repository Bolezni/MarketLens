package com.bolezni.mvp_test.authorization.store;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "_users")
@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    private String plan;

    @Column(nullable = false, columnDefinition = "int default 0", name = "token_version")
    private int tokenVersion;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
