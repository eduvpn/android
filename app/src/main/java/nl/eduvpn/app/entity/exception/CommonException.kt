package nl.eduvpn.app.entity.exception

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class CommonException(override val message: String?) : Exception(message) {

    fun translatedMessage() : String {
        if (message == null) {
            return toString()
        }
        return try {
            val parsedError: ExceptionMessage = json.decodeFromString(message)
            parsedError.message?.bestTranslation ?: message
        } catch (ex: Exception) {
            message
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}