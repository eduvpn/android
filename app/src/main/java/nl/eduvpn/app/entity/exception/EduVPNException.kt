package nl.eduvpn.app.entity.exception

import androidx.annotation.StringRes

/**
 * Exception with translatable message.
 *
 * @param formatArgs Arguments for [resourceIdMessage]
 */
class EduVPNException(
    @StringRes val resourceIdTitle: Int,
    @StringRes val resourceIdMessage: Int,
    vararg formatArgs: Any?
) : Exception() {
    val formatArgs = formatArgs
}
