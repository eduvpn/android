package nl.eduvpn.app.entity.exception

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class CommonException(override val message: String?) : Exception(message) {

    private val exceptionMessage: ExceptionMessage? get() {
        if (message == null) {
            return null
        }
        return try {
            json.decodeFromString(message)
        } catch (ex: Exception) {
            null
        }
    }

    fun translatedMessage() : String {
        if (message == null) {
            return toString()
        }
        return exceptionMessage?.message?.bestTranslation ?: message
    }

    fun isAuthCancellationException() : Boolean {
        return exceptionMessage?.message?.translations?.values?.any { it.contains("cause: context canceled") } == true
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}