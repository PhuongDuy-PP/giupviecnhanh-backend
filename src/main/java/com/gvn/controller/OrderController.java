package com.gvn.controller;

import com.gvn.dto.response.ApiResponse;
import com.gvn.dto.response.OptionResponse;
import com.gvn.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final ServiceService serviceService;
    
    /**
     * GET /api/v1/orders/option-source
     * Get options by source and service ID
     */
    @GetMapping("/option-source")
    public ResponseEntity<ApiResponse<List<OptionResponse>>> getOptionsBySource(
            @RequestParam("source") String source,
            @RequestParam("service_id") Integer serviceId,
            @RequestParam(value = "service_duration_time", required = false) Integer serviceDurationTime,
            @RequestParam(value = "start_hour", required = false) String startHour,
            @RequestParam(value = "from_date", required = false) String fromDate,
            @RequestParam(value = "to_date", required = false) String toDate
    ) {
        try {
            // Validate required parameters
            if (source == null || source.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("source is required", 400));
            }
            if (serviceId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("service_id is required", 400));
            }
            
            List<OptionResponse> options = serviceService.getOptionsBySource(
                    source, serviceId, serviceDurationTime, startHour, fromDate, toDate
            );
            
            return ResponseEntity.ok(
                    ApiResponse.success(options, "Success")
            );
        } catch (RuntimeException e) {
            log.error("Error retrieving options: ", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), 404));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Unexpected error retrieving options: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error", 500));
        }
    }
}
