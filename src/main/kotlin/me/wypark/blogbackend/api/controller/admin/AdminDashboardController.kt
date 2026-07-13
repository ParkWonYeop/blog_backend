package me.wypark.blogbackend.api.controller.admin

import me.wypark.blogbackend.api.common.ApiResponse
import me.wypark.blogbackend.application.dashboard.AdminDashboardResponse
import me.wypark.blogbackend.application.dashboard.AdminDashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    @GetMapping
    fun getDashboard(
        @RequestParam(required = false) range: String?,
        @RequestParam(required = false) timezone: String?
    ): ResponseEntity<ApiResponse<AdminDashboardResponse>> {
        val dashboard = adminDashboardService.getDashboard(range, timezone)
        return ResponseEntity.ok(ApiResponse.success(dashboard, "OK"))
    }
}
