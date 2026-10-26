package me.wypark.blogbackend.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import me.wypark.blogbackend.global.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** Bound API requests before MVC converters or the MCP WebMVC transport buffer them. */
class ApiRequestBodyLimitFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        val isImageUpload = request.method == "POST" && path == "/api/admin/images"
        val isMultipart = request.contentType?.substringBefore(';')?.trim()
            ?.equals(MediaType.MULTIPART_FORM_DATA_VALUE, ignoreCase = true) == true
        return !path.startsWith("/api/") || request.method !in BODY_METHODS || (isImageUpload && isMultipart)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.contentLengthLong > MAX_BODY_BYTES) {
            reject(response)
            return
        }

        val body = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)
        if (body.size > MAX_BODY_BYTES) {
            reject(response)
            return
        }
        filterChain.doFilter(CachedBodyRequest(request, body), response)
    }

    private fun reject(response: HttpServletResponse) {
        response.status = HttpStatus.PAYLOAD_TOO_LARGE.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(
            ApiResponse.error("요청 본문이 너무 큽니다.", "PAYLOAD_TOO_LARGE")
        ))
    }

    private class CachedBodyRequest(request: HttpServletRequest, private val body: ByteArray) :
        HttpServletRequestWrapper(request) {
        private val content = ByteArrayInputStream(body)

        override fun getContentLength(): Int = body.size

        override fun getContentLengthLong(): Long = body.size.toLong()

        override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
            override fun read(): Int = content.read()

            override fun isFinished(): Boolean = content.available() == 0

            override fun isReady(): Boolean = true

            override fun setReadListener(readListener: ReadListener) {
                throw UnsupportedOperationException("Request bodies use synchronous reads")
            }
        }

        override fun getReader(): BufferedReader = BufferedReader(
            InputStreamReader(inputStream, characterEncoding?.let(Charset::forName) ?: StandardCharsets.UTF_8)
        )
    }

    companion object {
        const val MAX_BODY_BYTES = 8 * 1024 * 1024
        private val BODY_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
