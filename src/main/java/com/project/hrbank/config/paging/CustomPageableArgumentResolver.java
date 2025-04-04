package com.project.hrbank.config.paging;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CustomPageableArgumentResolver extends PageableHandlerMethodArgumentResolver {

	private static final String PAGE = "page";
	private static final String SIZE = "size";
	private static final String SORT_FIELD_KEY = "sortField";
	private static final String SORT_DIRECTION_KEY = "sortDirection";

	@Override
	public Pageable resolveArgument(
		MethodParameter methodParameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		DefaultSortField defaultSortField = methodParameter.getMethodAnnotation(DefaultSortField.class);
		String sortFieldValue = extractingSortValue(defaultSortField, webRequest);
		Sort.Direction sortDirectionValue = getSortDirectionValue(webRequest);
		int pageValue = getIntegerOrDefault(webRequest.getParameter(PAGE), 0);
		int sizeValue = getIntegerOrDefault(webRequest.getParameter(SIZE), 30);

		return PageRequest.of(pageValue, sizeValue, Sort.by(sortDirectionValue, sortFieldValue));
	}

	private String extractingSortValue(DefaultSortField methodAnnotation, NativeWebRequest webRequest) {
		String sortField = webRequest.getParameter(SORT_FIELD_KEY);

		if (isSortFieldNull(sortField)) {
			return methodAnnotation.value();
		}

		return sortField;
	}

	private int getIntegerOrDefault(String value, int defaultValue) {
		if (isSortFieldNull(value)) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return defaultValue;
		}
	}

	private Sort.Direction getSortDirectionValue(NativeWebRequest webRequest) {
		String sortDirection = webRequest.getParameter(SORT_DIRECTION_KEY);
		if (isSortFieldNull(sortDirection)) {
			return Sort.Direction.DESC;
		}

		try {
			return Sort.Direction.fromString(sortDirection);
		} catch (IllegalArgumentException exception) {
			return Sort.Direction.DESC;
		}
	}

	private boolean isSortFieldNull(String sortField) {
		return sortField == null || sortField.isBlank();
	}

}
