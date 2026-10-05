package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "robots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stable hardware identifier the robot itself supplies (serial
    // number, config value...) - doubles as the WebSocket channel key
    // (ROBOT-06). Globally unique: one physical robot, one account.
    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    private String deviceId;

    // Who registered/owns this robot - only they can query or control it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RobotStatus status;

    // Which conversation session this robot is currently relaying for,
    // if any - lets an operator see "what is this robot doing right now".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_session_id")
    private ConversationSession currentSession;

    // Updated on every REST status update AND every WebSocket message.
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        lastSeenAt = LocalDateTime.now();

        if (status == null) {
            status = RobotStatus.OFFLINE;
        }
    }
}
