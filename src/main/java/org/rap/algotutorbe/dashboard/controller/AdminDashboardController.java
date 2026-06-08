package org.rap.algotutorbe.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.dashboard.dto.ActiveQuotaDto;
import org.rap.algotutorbe.dashboard.dto.AiTokenUsageDto;
import org.rap.algotutorbe.dashboard.dto.SystemOverviewDto;
import org.rap.algotutorbe.dashboard.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SystemOverviewDto>> getOverview() {
        SystemOverviewDto data = dashboardService.getSystemOverview();
        return ResponseEntity.ok(ApiResponse.buildSuccess(data));
    }

    @GetMapping("/ai-tokens")
    public ResponseEntity<ApiResponse<AiTokenUsageDto>> getAiTokenUsage(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        AiTokenUsageDto data = dashboardService.getAiTokenUsage(days);
        return ResponseEntity.ok(ApiResponse.buildSuccess(data));
    }

    @GetMapping("/api-quotas")
    public ResponseEntity<ApiResponse<List<ActiveQuotaDto>>> getApiQuotaUsage() {
        List<ActiveQuotaDto> data = dashboardService.getActiveApiQuotas();
        return ResponseEntity.ok(ApiResponse.buildSuccess(data));
    }
}
