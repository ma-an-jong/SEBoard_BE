package com.seproject.seboard.application;

import com.seproject.seboard.domain.model.Author;
import com.seproject.seboard.domain.model.Category;
import com.seproject.seboard.domain.model.Post;
import com.seproject.seboard.domain.model.UnnamedPost;
import com.seproject.seboard.domain.repository.*;
import com.seproject.seboard.domain.service.PostService;
import com.seproject.seboard.dto.PostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostAppService {
    //controller validation check -> 타입 맞는지, 공백 and null 체크
    //나머지는 application 아래에서
    private final PostService postService;

    private final PostRepository postRepository;
    private final UnnamedPostRepository unnamedPostRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public void createUnnamedPost(String title, String contents, Long categoryId, String username, String password){
        validatePost(title,contents,categoryId);
        Category category = findByIdOrThrow(categoryId,categoryRepository,"");
        Author author = Author.builder()
                .name(username)
                .loginId(Author.ANONYMOUS_LOGIN_ID)
                .build();

        UnnamedPost post = UnnamedPost.builder()
                .title(title)
                .contents(contents)
                .category(category)
                .author(author)
                .password(password)
                .build();

        authorRepository.save(author); //TODO : cascade 적용시 삭제 가능
        unnamedPostRepository.save(post);
    }

    public void createNamedPost(String title, String contents, Long categoryId, Long userId, boolean pined){
        validatePost(title,contents,categoryId);
        Author author = findByIdOrThrow(userId, authorRepository, "");
        Category category = findByIdOrThrow(categoryId,categoryRepository,"");
        Post post = Post.builder()
                .title(title)
                .contents(contents)
                .category(category)
                .author(author) //멤버 등록
                .pined(pined)
                .build();

        postRepository.save(post);
    }

    //TODO : 북마크 추가 -> 추가 완료
    public PostDTO.PostResponseDTO retrieveNamedPost(Long postId, Long userId){
        Post targetPost = findByIdOrThrow(postId, postRepository, "");
        Author requestUser = findByIdOrThrow(userId, authorRepository, "");
        boolean isEditable = false;
        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId,userId);

        if(!requestUser.isAnonymous()) {
            if (targetPost.isWrittenBy(requestUser)) { // TODO : 권한이 있을 때 추가
              isEditable = true;
            }
        }

        return PostDTO.PostResponseDTO.toDTO(targetPost,isEditable,bookmarked);
    }

    public PostDTO.PostResponseDTO retrieveUnnamedPost(Long postId, Long userId){
        UnnamedPost targetPost = findByIdOrThrow(postId, unnamedPostRepository, "");
        Author requestUser = findByIdOrThrow(userId, authorRepository, "");
        boolean isEditable = false;
        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId,userId);

        if(requestUser.isAnonymous()) {
            isEditable = true;
        }

        return PostDTO.PostResponseDTO.toDTO(targetPost,isEditable,bookmarked);
    }

    //pined 된거 상위 5개 먼저, 그다음에 최신순
    public List<PostDTO.PostListResponseDTO> retrievePostList(Long categoryId){
        //TODO : paging 인자, Repository 분리?
        //TODO : 추후 JPQL써서 개선, pined 구현 , categoryId 별로 정리
        return postRepository.findAll().stream().map(post -> {
            int commentSize = commentRepository.getCommentSizeByPostId(post.getPostId());
            return PostDTO.PostListResponseDTO.toDTO(post,commentSize);
        }).collect(Collectors.toList());
    }

    public void deleteNamedPost(Long postId, Long userId ) {
        // 권한 체크 -> 메소드 보안 체크 가능함 : AOP로 처리
        // 시큐리티 클라우드 적용 방법 -> 게이트웨이에 적용 가능
        Post targetPost = findByIdOrThrow(postId, postRepository, "");
        Author author = findByIdOrThrow(userId, authorRepository, "");

        //작성한 게시글의 작성자가 존재하지 않는 경우 TODO : message
        if(!targetPost.isWrittenBy(author)) {
            throw new IllegalArgumentException();
        }

        postRepository.deleteById(postId);
    }

    public void deleteUnnamedPost(Long postId, String password) {
        // 권한 체크 -> 메소드 보안 체크 가능함 : AOP로 처리
        // 시큐리티 클라우드 적용 방법 -> 게이트웨이에 적용 가능
        UnnamedPost targetPost = findByIdOrThrow(postId, unnamedPostRepository, "");

        // 비밀번호 확인
        if(!targetPost.checkPassword(password)) {
            throw new IllegalArgumentException();
        }

        postRepository.deleteById(postId);
    }

    // 게시글 수정
    public void updateNamedPost(String title,String contents,Long categoryId,Long postId,Long userId){
        validatePost(title,contents,categoryId);

        //게시물 및 요청자 id를 이용한 대상 조회
        Post targetPost = findByIdOrThrow(postId, postRepository, "");
        Author author = findByIdOrThrow(userId, authorRepository, "");
        Category category = findByIdOrThrow(categoryId,categoryRepository,"");

        // 동일 인물인지 검사
        if(!targetPost.isWrittenBy(author)) {
            throw new IllegalArgumentException();
        }
        // 게시글 업데이트
        targetPost.update(title,contents,category);
    }

    public void updateUnnamedPost(String title,String contents,Long categoryId,Long postId, String password){
        validatePost(title,contents,categoryId);

        //게시물 및 요청자 id를 이용한 대상 조회
        UnnamedPost targetPost = findByIdOrThrow(postId, unnamedPostRepository, "");
        Category category = findByIdOrThrow(categoryId,categoryRepository,"");

        //비밀번호 검사
        if(!targetPost.checkPassword(password)) {
            throw new IllegalArgumentException();
        }

        // 게시글 업데이트 -> TODO : 익명일때 비밀번호 수정도 필요한가?
        targetPost.update(title,contents,category);
    }

    private <T> T findByIdOrThrow(Long id, JpaRepository<T, Long> repo, String errorMsg){
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException(errorMsg));
    }

    private void validatePost(String title,String contents,Long categoryId){

        //카테고리 존재하는지 확인 TODO : message 추가
        if(!postService.existCategory(categoryId)) {
            throw new NoSuchElementException();
        }

        //제목 길이 초과여부 확인
        if(!postService.isValidTitle(title)) {
            throw new IllegalArgumentException();
        }

        //본문 길이 초과여부 확인
       if(!postService.isValidContents(contents)) {
            throw new IllegalArgumentException();
        }

    }

}
