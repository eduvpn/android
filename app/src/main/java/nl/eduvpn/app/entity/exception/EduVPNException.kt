package nl.eduvpn.app.entity.exception

import androidx.annotation.StringRes

/**
 * Exception with translatable message.
 */
data class EduVPNException(
    @StringRes val resourceIdTitle: Int,
    @StringRes val resourceIdMessage: Int,
    val throwable: Throwable? = null
) : Exception(throwable)
