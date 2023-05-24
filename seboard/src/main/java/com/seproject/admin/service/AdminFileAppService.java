package com.seproject.admin.service;

import com.seproject.account.model.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.controller.dto.file.FileResponse;
import com.seproject.admin.controller.dto.file.FileResponse.FileExtensionResponse;
import com.seproject.admin.domain.FileExtension;
import com.seproject.admin.domain.repository.FileExtensionRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.seboard.application.utils.FileUtils;
import com.seproject.seboard.domain.repository.commons.FileMetaDataRepository;
import com.seproject.seboard.domain.repository.commons.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFileAppService {
    private final FileUtils fileUtils;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileRepository fileRepository;
    private final FileExtensionRepository fileExtensionRepository;

    public void deleteBulkFile(List<Long> fileIds) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        fileMetaDataRepository.findAllById(fileIds).forEach(fileMetaData -> {
            fileRepository.delete(fileMetaData.getFilePath());
            fileMetaDataRepository.delete(fileMetaData);
        });
    }

    public void addFileExtension(List<String> extensions) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        //TODO : 중복 검사?
        extensions.forEach(extension -> {
            FileExtension fileExtension = new FileExtension(extension);
            fileUtils.addAllowedExtension(extension);
            fileExtensionRepository.save(fileExtension);
        });
    }

    public void removeFileExtension(List<String> extensions){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        //TODO : 중복 검사?
        extensions.forEach(extension -> {
            fileUtils.removeAllowedExtension(extension);
            fileExtensionRepository.removeByExtensionName(extension);
        });
    }

    public FileExtensionResponse retrieveFileExtension(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        return FileExtensionResponse.of(fileExtensionRepository.findAll());
    }
}