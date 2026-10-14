package ru.grabovsky.drinkmebot.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "user_profile", schema = "drink_me_bot")
data class UserProfile(
    @Id
    @Column(name = "user_id")
    var userId: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(name = "is_blocked", nullable = false)
    var isBlocked: Boolean = false,

    @Column(name = "is_admin", nullable = false)
    var isAdmin: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    var settings: UserSettings = UserSettings(),

    @Column(name = "locale")
    var locale: String? = null,
)

data class UserSettings(
    /** Удалять служебные сообщения: команды в личке и временные подтверждения бота. */
    var cleanChat: Boolean = true,
    /** Язык коктейлей (код локали данных, см. `DataLocale.SUPPORTED`); `null` - определять по языку Telegram. */
    var cocktailLang: String? = null,
)
