package com.project.hrbank.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.hrbank.dto.response.CursorPageResponse;
import com.project.hrbank.dto.response.EmployeeLogResponse;
import com.project.hrbank.service.EmployeeLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/change-logs")
@RequiredArgsConstructor
@Tag(name = "직원 정보 수정 이력 관리", description = "직원 정보 수정 이력 관리 API")
public class EmployeeLogRestController {

	private final EmployeeLogService service;

	private static final Map<String, String> FIELD_MAP = Map.of(
		"at", "changedAt",
		"type", "type",
		"ipAddress", "ipAddress"
	);

	/**
	 *
	 * @param employeeNumber - 사원 번호 검색
	 * @param memo - 메모 검색
	 * @param ipAddress - IP 주소 검색
	 * @param type - 로그 타입(CREATE || UPDATE || DELETE)
	 * @param atFrom - 날짜 검색( 시작일 )
	 * @param atTo - 날짜 검색( 종료일 )
	 * @param size - 페이지 당 로딩 개수
	 * @param sortField - 정렬 기준
	 * @param sortDirection - 정렬 ASC, DESC
	 * @param cursor
	 * @return Log List 반환
	 */
	@GetMapping
	@Operation(summary = "직원 정보 수정 이력 목록 조회", description = "직원 정보 수정 이력 목록을 조회합니다. 상세 변경 내용은 포함되지 않습니다.")
	public CursorPageResponse<EmployeeLogResponse> getLogList(
		@RequestParam(defaultValue = "") String employeeNumber,
		@RequestParam(defaultValue = "") String memo,
		@RequestParam(defaultValue = "") String ipAddress,
		@RequestParam(defaultValue = "") String type,
		@RequestParam(value = "atFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime atFrom,
		@RequestParam(value = "atTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime atTo,
		@RequestParam(defaultValue = "30") int size,
		@RequestParam(defaultValue = "at") String sortField,
		@RequestParam(defaultValue = "desc") String sortDirection,
		@RequestParam(required = false) LocalDateTime cursor
	) {

		// 정렬 필드 매핑
		String mappedField = FIELD_MAP.getOrDefault(sortField, "changed_at");

		// 정렬 방향 설정
		Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sort = Sort.by(direction, mappedField);

		// Pageable 설정 후 데이터 조회
		Pageable pageable = PageRequest.of(0, size, sort);

		return service.getLogs(cursor, employeeNumber, memo, ipAddress, type, atFrom, atTo, pageable);
	}

	/**
	 *
	 * @param id - 상세 변경사항 검색 id
	 * @return 변경 상세 내역 반환
	 */
	@GetMapping("{id}/diffs")
	public String getLogById(@PathVariable Long id) {
		return service.getLogById(id);
	}

	/**
	 *
	 * @return 변경된 목록 개수 반환
	 */
	@GetMapping("/count")
	public long getLogCount() {
		return service.getLogCount();
	}
}
