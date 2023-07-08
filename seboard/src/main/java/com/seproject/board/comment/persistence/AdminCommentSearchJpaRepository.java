package com.seproject.board.comment.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.board.comment.domain.repository.AdminCommentSearchRepository;
import com.seproject.board.post.controller.PostSearchOptions;
import com.seproject.board.common.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;

import java.util.List;

import static com.seproject.board.comment.controller.dto.AdminCommentRequest.*;
import static com.seproject.board.comment.controller.dto.AdminCommentResponse.*;
import static com.seproject.board.comment.domain.model.QComment.comment;

@Repository
public class AdminCommentSearchJpaRepository implements AdminCommentSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Autowired
    public AdminCommentSearchJpaRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<AdminCommentListResponse> findCommentListByCondition(AdminCommentRetrieveCondition condition, Pageable pageable) {
        List<AdminCommentListResponse> contents = queryFactory
                .select(Projections.constructor(AdminCommentListResponse.class,
                        comment))
                .from(comment)
                .where(
                        searchOption(condition.getSearchOption(), condition.getQuery()),
                        reportedStatusEq(condition.getIsReported()),
                        readOnlyAuthorEq(condition.getIsReadOnlyAuthor())
                ).orderBy(comment.baseTime.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        searchOption(condition.getSearchOption(), condition.getQuery()),
                        reportedStatusEq(condition.getIsReported()),
                        readOnlyAuthorEq(condition.getIsReadOnlyAuthor())
                )
                .fetchOne();

        return new PageImpl<>(contents, pageable, count);
    }

    @Override
    public Page<AdminDeletedCommentResponse> findDeletedCommentList(Pageable pageable) {
        List<AdminDeletedCommentResponse> content = queryFactory
                .select(Projections.constructor(AdminDeletedCommentResponse.class,
                        comment))
                .from(comment)
                .where(comment.status.eq(Status.TEMP_DELETED))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(comment.baseTime.createdAt.desc())
                .fetch();

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.status.eq(Status.TEMP_DELETED))
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }

    private BooleanExpression readOnlyAuthorEq(Boolean isReadOnlyAuthor){
        if(isReadOnlyAuthor == null){
            return null;
        }else if(isReadOnlyAuthor){
            return comment.isOnlyReadByAuthor.isTrue();
        }else{
            return comment.isOnlyReadByAuthor.isFalse();
        }
    }

    private BooleanExpression reportedStatusEq(Boolean isReported){
        if(isReported == null){
            return comment.status.eq(Status.NORMAL)
                    .or(comment.status.eq(Status.REPORTED));
        }else if(!isReported){
            return comment.status.eq(Status.NORMAL);
        } else{
            return comment.status.eq(Status.REPORTED);
        }
    }

    private BooleanExpression searchOption(String searchOption, String query){
        if(searchOption==null || query==null){
            return null;
        }

        BooleanExpression contentOption = comment.contents.contains(query);
        BooleanExpression authorOption = comment.author.name.contains(query);

        switch (PostSearchOptions.valueOf(searchOption)){
            case CONTENT:{
                return contentOption;
            }
            case AUTHOR:{
                return authorOption;
            }
            case ALL:{
                return contentOption
                        .or(authorOption);
            }
            default:{
                return null;
            }
        }
    }
}
