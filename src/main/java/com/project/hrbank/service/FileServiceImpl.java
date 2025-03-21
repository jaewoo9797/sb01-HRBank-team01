package com.project.hrbank.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.project.hrbank.entity.FileEntity;
import com.project.hrbank.repository.FileRepository;
import com.project.hrbank.util.factory.FileHandlerFactory;
import com.project.hrbank.util.handler.FileHandler;
import com.project.hrbank.util.storage.FileStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
	private final FileRepository fileRepository;
	private final FileStorage fileStorage;
	private final FileHandlerFactory fileHandlerFactory;

	@Override
	public FileEntity saveMultipartFile(MultipartFile file) throws IOException {
		if (file == null) {
			return null;
		}

		String fileName = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "unknown_file";
		FileHandler fileHandler = fileHandlerFactory.getFileHandler(fileName);
		byte[] processedFileData = fileHandler.processMultipartFile(file);

		FileEntity fileEntity = fileStorage.saveFile(
			null,
			processedFileData,
			file.getOriginalFilename(),
			file.getContentType()
		);
		return fileRepository.save(fileEntity);
	}

	@Override
	public FileEntity saveFileData(String fileName, byte[] fileData, String contentType) throws IOException {
		if (fileData == null || fileData.length == 0) {
			throw new IllegalArgumentException("파일 데이터가 비어 있습니다.");
		}
		FileHandler fileHandler = fileHandlerFactory.getFileHandler(fileName);
		byte[] processedFileData = fileHandler.processFileData(fileName, fileData);

		FileEntity fileEntity = fileStorage.saveFile(
			null,
			processedFileData,
			fileName,
			contentType
		);
		return fileRepository.save(fileEntity);
	}

	@Override
	public FileEntity find(Long fileId) {
		return fileRepository.findById(fileId)
			.orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
	}

	@Override
	public FileEntity updateFile(Long fileId, MultipartFile newFile) throws IOException {
		if (newFile.isEmpty()) {
			throw new IllegalArgumentException("업로드된 새 파일이 비어있습니다.");
		}
		FileEntity existFile = find(fileId);
		fileStorage.delete(existFile.getId());
		fileRepository.delete(existFile);

		String fileName = (newFile.getOriginalFilename() != null) ? newFile.getOriginalFilename() : "unknown_file";

		byte[] processedFileData = fileHandlerFactory.getFileHandler(fileName).processMultipartFile(newFile);

		FileEntity updateFile = fileStorage.saveFile(
			null,
			processedFileData,
			newFile.getOriginalFilename(),
			newFile.getContentType()
		);
		return fileRepository.save(updateFile);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteFile(Long fileId) {
		FileEntity findEntity = fileRepository.findById(fileId)
			.orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));

		fileRepository.delete(findEntity);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				fileStorage.delete(fileId); // 이제 이건 디스크 삭제만 담당하니까 안전
			}
		});
	}
}
