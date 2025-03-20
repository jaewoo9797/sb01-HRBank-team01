package com.project.hrbank.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.hrbank.dto.DepartmentDto;
import com.project.hrbank.dto.response.CursorPageResponse;
import com.project.hrbank.service.DepartmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

	private final DepartmentService departmentService;

	@PostMapping
	public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto dto) {
		DepartmentDto createdDepartment = departmentService.createDepartment(dto);
		return new ResponseEntity<>(createdDepartment, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
		try {
			DepartmentDto department = departmentService.getDepartmentById(id);
			return ResponseEntity.ok(department);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping
	public ResponseEntity<CursorPageResponse<DepartmentDto>> getAllDepartments(
		@RequestParam(required = false) LocalDateTime cursor,
		@RequestParam(defaultValue = "") String nameOrDescription,
		@RequestParam(defaultValue = "name") String sortField,
		@RequestParam(defaultValue = "asc") String sortDirection,
		@RequestParam(defaultValue = "30") int size
	) {

		String searchQuery = nameOrDescription != null && !nameOrDescription.trim().isEmpty()
			? nameOrDescription.trim()
			: "";

		Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sort = Sort.by(direction, sortField);
		Pageable pageable = PageRequest.of(0, size, sort);

		CursorPageResponse<DepartmentDto> departments = departmentService.getAllDepartments(cursor, searchQuery,
			pageable);
		return ResponseEntity.ok(departments);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDto dto) {
		DepartmentDto updatedDepartment = departmentService.updateDepartment(id, dto);
		return ResponseEntity.ok(updatedDepartment);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
		departmentService.deleteDepartment(id);
		return ResponseEntity.noContent().build();
	}
}
