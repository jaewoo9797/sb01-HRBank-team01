package com.project.hrbank.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.hrbank.dto.DepartmentDto;
import com.project.hrbank.dto.response.CursorPageResponse;
import com.project.hrbank.entity.Department;
import com.project.hrbank.repository.DepartmentRepository;
import com.project.hrbank.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

	private final DepartmentRepository departmentRepository;
	private final EmployeeRepository employeeRepository;

	@Override
	@Transactional
	public DepartmentDto createDepartment(DepartmentDto dto) {
		if (departmentRepository.existsByName(dto.name())) {
			throw new IllegalArgumentException("Department with name " + dto.name() + " already exists.");
		}

		Department department = new Department();
		department.update(dto.name(), dto.description(), dto.establishedDate());

		departmentRepository.save(department);

		return new DepartmentDto(
			department.getId(),
			department.getName(),
			department.getDescription(),
			department.getEstablishedDate(),
			employeeRepository.countEmployeesByDepartmentId(department.getId()),
			department.getCreatedAt()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public DepartmentDto getDepartmentById(Long id) {
		Department department = departmentRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + id));

		return new DepartmentDto(
			department.getId(),
			department.getName(),
			department.getDescription(),
			department.getEstablishedDate(),
			getEmployeeCount(department.getId()),
			department.getCreatedAt()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPageResponse<DepartmentDto> getAllDepartments(
		LocalDateTime cursor,
		String nameOrDescription,
		Pageable pageable
	) {
		cursor = null;

		String searchQuery = nameOrDescription.isBlank() ? "" : nameOrDescription.trim();

		Page<Department> page = departmentRepository.findNextDepartments(
			cursor,
			searchQuery,
			pageable
		);

		List<DepartmentDto> content = getDepartmentContents(page);

		LocalDateTime nextCursor = null;
		if (page.hasContent()) {
			nextCursor = content.get(content.size() - 1).createdAt();
		}

		Long nextIdAfter = null;
		if (page.hasNext() && page.hasContent()) {
			nextIdAfter = content.get(content.size() - 1).id();
		}

		return new CursorPageResponse<>(
			content,
			nextCursor,
			nextIdAfter,
			content.size(),
			page.hasNext(),
			page.getTotalElements()
		);
	}

	private List<DepartmentDto> getDepartmentContents(Page<Department> slice) {
		return slice.getContent()
			.stream()
			.map(department -> new DepartmentDto(
				department.getId(),
				department.getName(),
				department.getDescription(),
				department.getEstablishedDate(),
				getEmployeeCount(department.getId()),
				department.getCreatedAt()
			))
			.toList();
	}

	@Override
	@Transactional
	public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
		Department department = departmentRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Department not found"));

		if (!department.getName().equals(dto.name()) && departmentRepository.existsByName(dto.name())) {
			throw new IllegalArgumentException("Department name already exists");
		}

		department.update(dto.name(), dto.description(), dto.establishedDate());

		return new DepartmentDto(
			department.getId(),
			department.getName(),
			department.getDescription(),
			department.getEstablishedDate(),
			getEmployeeCount(department.getId()),
			department.getCreatedAt()
		);
	}

	@Override
	@Transactional
	public void deleteDepartment(Long id) {
		Department department = departmentRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Department not found"));

		if (getEmployeeCount(id) > 0) {
			throw new IllegalStateException("Cannot delete department with existing employees");
		}

		departmentRepository.delete(department);
	}

	private long getEmployeeCount(Long departmentId) {
		return employeeRepository.countEmployeesByDepartmentId(departmentId);
	}
}
