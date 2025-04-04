package com.project.hrbank.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;

import com.project.hrbank.dto.request.EmployeeLogRequest;
import com.project.hrbank.dto.response.CursorPageResponse;
import com.project.hrbank.dto.response.EmployeeLogResponse;

public interface EmployeeLogService {
	CursorPageResponse<EmployeeLogResponse> getLogs(LocalDateTime cursor, String employeeNumber, String memo,
		String ipAddress, String type, LocalDateTime atFrom, LocalDateTime atTo, Pageable pageable);

	String getLogById(Long id);

	long getLogCount();

	EmployeeLogResponse createLog(EmployeeLogRequest request);

}
