package com.seproject.admin.service;

import com.seproject.account.model.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.controller.dto.post.AdminPostRequest;
import com.seproject.admin.controller.dto.post.AdminPostResponse;
import com.seproject.admin.domain.repository.AdminPostSearchRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.seboard.domain.model.user.BoardUser;
import com.seproject.seboard.domain.model.post.Post;
import com.seproject.seboard.domain.repository.report.ReportRepository;
import com.seproject.seboard.domain.repository.user.BoardUserRepository;
import com.seproject.seboard.domain.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.seproject.seboard.application.utils.AppServiceHelper.findByIdOrThrow;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostAppService {

    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final BoardUserRepository boardUserRepository;
//    private final AdminPostSearchRepository adminPostSearchRepository;

    public Page<AdminPostResponse> findPostList(int page, int size, Long categoryId, String exposeOption, boolean isReported, String searchType, String query) {

        return null;
    }

    public void enrollPin(Long accountId, Long postId) {
        BoardUser requestUser = findByIdOrThrow(accountId, boardUserRepository, "");
        //TODO: 권한처리

        Post post = findByIdOrThrow(postId, postRepository, "");
        post.changePin(true);
    }

    public void cancelPin(Long accountId, Long postId) {
        BoardUser requestUser = findByIdOrThrow(accountId, boardUserRepository, "");
        //TODO: 권한처리

        Post post = findByIdOrThrow(postId, postRepository, "");
        post.changePin(false);
    }

    public void restorePost(Long postId){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));

        post.restore();
        reportRepository.deleteAllByPostId(post.getPostId());
    }

    public void restoreBulkPost(List<Long> postIds) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        postRepository.findAllById(postIds).forEach(post -> {
            post.restore();
            reportRepository.deleteAllByPostId(post.getPostId());
        });
    }

    public void deleteBulkPost(List<Long> postIds) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new InvalidAuthorizationException(ErrorCode.NOT_LOGIN));

        boolean isAdmin = account.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin){
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        postRepository.findAllById(postIds).forEach(post -> {
            post.delete(false);
        });
    }

}