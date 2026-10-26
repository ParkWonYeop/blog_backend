package me.wypark.blogbackend.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ApiRequestBodyLimitFilterTest {
    private val filter = ApiRequestBodyLimitFilter(ObjectMapper())

    @Test
    fun `image multipart retains its servlet upload limit without exempting other endpoints`() {
        val oversized = ByteArray(ApiRequestBodyLimitFilter.MAX_BODY_BYTES + 1)
        val imageUpload = MockHttpServletRequest("POST", "/api/admin/images").apply {
            contentType = MediaType.MULTIPART_FORM_DATA_VALUE
            setContent(oversized)
        }
        val uploadChain = MockFilterChain()
        filter.doFilter(imageUpload, MockHttpServletResponse(), uploadChain)
        assertSame(imageUpload, uploadChain.request)

        val otherEndpoint = MockHttpServletRequest("POST", "/api/auth/login").apply {
            contentType = MediaType.MULTIPART_FORM_DATA_VALUE
            setContent(oversized)
        }
        val response = MockHttpServletResponse()
        filter.doFilter(otherEndpoint, response, MockFilterChain())
        assertEquals(413, response.status)
    }
}
