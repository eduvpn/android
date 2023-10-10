package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CertExpiryTimes(
    @SerialName("start_time")
    val startTime: Long? = null,
    @SerialName("end_time")
    val endTime: Long? = null,
    @SerialName("button_time")
    val buttonTime: Long? = null,
    @SerialName("countdown_time")
    val countdownTime: Long? = null,
    @SerialName("notification_times")
    val notificationTimes: List<Long> = emptyList(),
)