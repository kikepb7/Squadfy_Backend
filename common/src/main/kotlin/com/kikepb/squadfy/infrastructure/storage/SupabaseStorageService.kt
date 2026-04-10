package com.kikepb.squadfy.infrastructure.storage

import com.kikepb.squadfy.domain.exception.StorageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class SupabaseStorageService(
    private val supabaseRestClient: RestClient,
    @Value("\${supabase.project-url}") private val projectUrl: String
) {
    private val logger = LoggerFactory.getLogger(SupabaseStorageService::class.java)

    companion object {
        val ALLOWED_IMAGE_MIME_TYPES = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg"  to "jpg",
            "image/png"  to "png",
            "image/webp" to "webp"
        )
    }

    fun uploadImage(bucket: String, folder: String, bytes: ByteArray, mimeType: String): String {
        val extension = ALLOWED_IMAGE_MIME_TYPES[mimeType]
            ?: throw StorageException("Unsupported image type: $mimeType. Allowed: ${ALLOWED_IMAGE_MIME_TYPES.keys}")

        val storagePath = "$folder/${UUID.randomUUID()}.$extension"

        try {
            supabaseRestClient.post()
                .uri("/storage/v1/object/$bucket/$storagePath")
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(bytes)
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            throw StorageException("Failed to upload image to $bucket/$storagePath: ${e.message}")
        }

        val url = publicUrl(bucket, storagePath)
        logger.info("Image uploaded: $url")
        return url
    }


    fun createSignedUploadUrl(storagePath: String, expiresInSeconds: Int): String {
        val response = supabaseRestClient.post()
            .uri("/storage/v1/object/upload/sign/$storagePath")
            .header("Content-Type", "application/json")
            .body("""{"expiresIn": $expiresInSeconds}""")
            .retrieve()
            .body(SignedUploadResponse::class.java)
            ?: throw StorageException("Failed to create signed upload URL for $storagePath")

        return "$projectUrl/storage/v1${response.url}"
    }

    fun deleteFile(url: String) {
        val path = if (url.contains("/object/public/")) {
            url.substringAfter("/object/public/")
        } else {
            throw StorageException("Invalid Supabase Storage URL format: $url")
        }

        val response = supabaseRestClient.delete()
            .uri("/storage/v1/object/$path")
            .retrieve()
            .toBodilessEntity()

        if (response.statusCode.isError) {
            throw StorageException("Unable to delete file: ${response.statusCode.value()}")
        }

        logger.info("File deleted from Supabase Storage: $url")
    }

    fun publicUrl(bucket: String, storagePath: String): String =
        "$projectUrl/storage/v1/object/public/$bucket/$storagePath"

    private data class SignedUploadResponse(val url: String)
}
