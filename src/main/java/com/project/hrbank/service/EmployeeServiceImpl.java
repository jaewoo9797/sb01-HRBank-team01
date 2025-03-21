package com.project.hrbank.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.hrbank.dto.DepartmentDto;
import com.project.hrbank.dto.request.EmployeeRequestDto;
import com.project.hrbank.dto.response.EmployeeResponseDto;
import com.project.hrbank.entity.Employee;
import com.project.hrbank.entity.EmployeeStatus;
import com.project.hrbank.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.hrbank.util.IpUtils;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

	private final JdbcTemplate jdbcTemplate;

	private final EmployeeRepository employeeRepository;
	private final DepartmentService departmentService;
	private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

	@Override
	public EmployeeResponseDto registerEmployee(EmployeeRequestDto requestDto, MultipartFile profileImage) {
		if (employeeRepository.existsByEmail(requestDto.getEmail())) {
			throw new IllegalArgumentException("중복된 이메일입니다.");
		}
		String employeeNumber = generateEmployeeNumber();

		Employee employee = Employee.builder()
			.employeeNumber(employeeNumber)
			.name(requestDto.getName())
			.email(requestDto.getEmail())
			.departmentId(requestDto.getDepartmentId())
			.position(requestDto.getPosition())
			.hireDate(requestDto.getHireDate())
			.status(EmployeeStatus.ACTIVE)
			.build();

		if (profileImage != null && !profileImage.isEmpty()) {
			Long profileImageId = saveProfileImage(profileImage);
			employee.setProfileImageId(profileImageId);
		}

		Employee savedEmployee = employeeRepository.save(employee);

		List<Map<String, Object>> logData = new ArrayList<>();
		logData.add(createLogEntry("employee_number", null, savedEmployee.getEmployeeNumber()));
		logData.add(createLogEntry("hire_date", null, employee.getHireDate().toString()));
		logData.add(createLogEntry("name", null, employee.getName()));
		logData.add(createLogEntry("position", null, employee.getPosition()));
		logData.add(createLogEntry("department", null, String.valueOf(employee.getDepartmentId())));
		logData.add(createLogEntry("email", null, employee.getEmail()));
		logData.add(createLogEntry("status", null, employee.getStatus().toString()));
		if (requestDto.getMemo() != null && !requestDto.getMemo().isEmpty()) {
			logData.add(createLogEntry("memo", null, requestDto.getMemo()));
		}

		saveLog("CREATED", logData, savedEmployee.getEmployeeNumber(), requestDto.getMemo());

		return convertToDto(employee);
	}

	@Override
	public Page<EmployeeResponseDto> getEmployees(String nameOrEmail, String departmentName, String position,
		EmployeeStatus status, int page, int size, String sortField, String sortDirection) {
		Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
		Pageable pageable = PageRequest.of(page, size, sort);

		Page<Employee> employees = employeeRepository.findFilteredEmployees(
			departmentName,
			position,
			status,
			pageable
		);

		return employees.map(this::convertToDto);
	}

	@Override
	public EmployeeResponseDto getEmployeeById(Long id) {
		return employeeRepository.findById(id)
			.map(this::convertToDto)
			.orElse(null);
	}

	@Override
	public long countEmployeesHiredInDateRange(LocalDate fromDate, LocalDate toDate) {
		return employeeRepository.countByHireDateBetween(fromDate, toDate);
	}

	//@Transactional 사용위치 확인 후 수정 클래스? 메서드? // 코드 컨벤션 지켜서 작성하기
	@Override
	@Transactional
	public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto, MultipartFile profileImage) {
		Employee existingEmployee = employeeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

		List<Map<String, Object>> logData = new ArrayList<>();

		if (!Objects.equals(existingEmployee.getHireDate(), dto.getHireDate())) {
			logData.add(createLogEntry("hire_date",
				existingEmployee.getHireDate() != null ? existingEmployee.getHireDate().toString() : null,
				dto.getHireDate() != null ? dto.getHireDate().toString() : null));
			existingEmployee.setHireDate(dto.getHireDate());
		}

		if (!Objects.equals(existingEmployee.getName(), dto.getName())) {
			logData.add(createLogEntry("name", existingEmployee.getName(), dto.getName()));
			existingEmployee.setName(dto.getName());
		}

		if (!Objects.equals(existingEmployee.getPosition(), dto.getPosition())) {
			logData.add(createLogEntry("position", existingEmployee.getPosition(), dto.getPosition()));
			existingEmployee.setPosition(dto.getPosition());
		}

		if (!Objects.equals(existingEmployee.getDepartmentId(), dto.getDepartmentId())) {
			logData.add(createLogEntry("department",
				existingEmployee.getDepartmentId() != null ? String.valueOf(existingEmployee.getDepartmentId()) : null,
				dto.getDepartmentId() != null ? String.valueOf(dto.getDepartmentId()) : null));
			existingEmployee.setDepartmentId(dto.getDepartmentId());
		}

		if (!Objects.equals(existingEmployee.getEmail(), dto.getEmail())) {
			logData.add(createLogEntry("email", existingEmployee.getEmail(), dto.getEmail()));
			existingEmployee.setEmail(dto.getEmail());
		}

		if (!Objects.equals(existingEmployee.getStatus(), dto.getStatus())) {
			logData.add(createLogEntry("status",
				existingEmployee.getStatus() != null ? existingEmployee.getStatus().toString() : null,
				dto.getStatus() != null ? dto.getStatus().toString() : null));
			existingEmployee.setStatus(dto.getStatus());
		}

		// 프로필 이미지 처리
		if (profileImage != null && !profileImage.isEmpty()) {
			Long profileImageId = saveProfileImage(profileImage);
			logData.add(createLogEntry("profile_image",
				existingEmployee.getProfileImageId() != null ? String.valueOf(existingEmployee.getProfileImageId()) :
					null,
				profileImageId != null ? String.valueOf(profileImageId) : null));
			existingEmployee.setProfileImageId(profileImageId);
		}

		String employeeNumber = existingEmployee.getEmployeeNumber();

		saveLog("UPDATED", logData, employeeNumber, dto.getMemo());

		return convertToDto(existingEmployee);
	}

	//파일 저장예시코드
	private Long saveProfileImage(MultipartFile profileImage) {
		try {
			String fileName = profileImage.getOriginalFilename();
			byte[] fileBytes = profileImage.getBytes();

			return 123L;
		} catch (IOException e) {
			throw new RuntimeException("프로필 이미지를 저장하는 데 실패했습니다.", e);
		}
	}

	@Override
	public void deleteEmployee(Long id) {
		Employee employee = employeeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

		List<Map<String, Object>> logData = new ArrayList<>();

		logData.add(createLogEntry("employee_number", employee.getEmployeeNumber(), null));
		logData.add(createLogEntry("hire_date",
			employee.getHireDate() != null ? employee.getHireDate().toString() : null,
			null));
		logData.add(createLogEntry("name", employee.getName(), null));
		logData.add(createLogEntry("position", employee.getPosition(), null));
		logData.add(createLogEntry("department",
			employee.getDepartmentId() != null ? String.valueOf(employee.getDepartmentId()) : null,
			null));
		logData.add(createLogEntry("email", employee.getEmail(), null));
		logData.add(createLogEntry("status",
			employee.getStatus() != null ? employee.getStatus().toString() : null,
			null));

		saveLog("DELETED", logData, employee.getEmployeeNumber(), "직원 삭제");
		employeeRepository.deleteById(id);
	}

	private String generateEmployeeNumber() {
		long count = employeeRepository.count() + 1;
		return String.format("EMP%03d", count);

	}

	@Override
	public long countEmployees(EmployeeStatus status, String fromDate, String toDate) {
		LocalDate start = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate) : null;
		LocalDate end = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate) : null;

		return employeeRepository.countEmployees(status, start, end);
	}

	@Override
	public List<Map<String, Object>> getEmployeeStatsTrend(LocalDate from, LocalDate to, String unit) {

		LocalDate oldestHireDate = employeeRepository.findOldestHireDate();
		LocalDate startDate = (from != null) ? from : oldestHireDate;
		LocalDate endDate = (to != null) ? to : LocalDate.now();

		List<Object[]> results = employeeRepository.calculateStatsByUnit(startDate, endDate, unit);

		List<Map<String, Object>> trendData = new ArrayList<>();
		for (Object[] result : results) {
			Map<String, Object> entry = new HashMap<>();
			entry.put("date", result[0]);
			entry.put("count", result[1]);
			entry.put("totalEmployees", result[2]);
			trendData.add(entry);
		}

		return trendData;
	}

	private void saveLog(String type, List<Map<String, Object>> logEntries, String employeeNumber, String memo) {

		try {
			String jsonString = new ObjectMapper().writeValueAsString(logEntries);
			logger.info("로그 출력: {}", jsonString);

			String sql = """
				INSERT INTO employee_change_logs 
				    (type, changed_value, ip, employee_number, changed_at, memo)
				VALUES 
				    (?, ?::jsonb, ?, ?, ?, ?)
				""";

			jdbcTemplate.update(
				sql,
				type,
				jsonString,
				IpUtils.getClientIp(),
				employeeNumber,
				LocalDateTime.now(),
				memo
			);

		} catch (JsonProcessingException e) {
			logger.error("JSON 변환 실패", e);
		}
	}

	private Map<String, Object> createLogEntry(String propertyName, String before, String after) {
		Map<String, Object> entry = new HashMap<>();
		entry.put("propertyName", propertyName);
		entry.put("before", before);
		entry.put("after", after);
		return entry;
	}

	@Override
	public List<Map<String, Object>> getEmployeeDistribution(String groupBy, EmployeeStatus status) {
		List<Object[]> results;

		switch (groupBy.toLowerCase()) {
			case "department":
				results = employeeRepository.countEmployeesGroupedByDepartment(status);
				break;
			case "position":
				results = employeeRepository.countEmployeesGroupedByPosition(status);
				break;
			default:
				throw new IllegalArgumentException("부서코드는 필수입니다. " + groupBy);
		}

		long totalCount = employeeRepository.countByStatus(status);

		List<Map<String, Object>> distribution = new ArrayList<>();
		for (Object[] result : results) {
			String key = (String)result[0];
			Long count = (Long)result[1];
			double percentage = ((double)count / totalCount) * 100;

			Map<String, Object> entry = new HashMap<>();
			entry.put("groupKey", key);
			entry.put("count", count);
			entry.put("percentage", Math.round(percentage * 100.0) / 100.0);
			distribution.add(entry);
		}

		return distribution;
	}

	private EmployeeResponseDto convertToDto(Employee employee) {
		DepartmentDto departmentDto = departmentService.getDepartmentById(employee.getDepartmentId());

		return EmployeeResponseDto.builder()
			.id(employee.getEmployeeId())
			.name(employee.getName())
			.email(employee.getEmail())
			.employeeNumber(employee.getEmployeeNumber())
			.departmentId(employee.getDepartmentId())
			.departmentName(departmentDto.name())
			.position(employee.getPosition())
			.hireDate(employee.getHireDate())
			.status(employee.getStatus())
			.profileImageId(employee.getProfileImageId())
			.createdAt(employee.getCreatedAt())
			.build();
	}
}
