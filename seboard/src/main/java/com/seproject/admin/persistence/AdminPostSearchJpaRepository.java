package com.seproject.admin.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.controller.dto.post.AdminPostRequest.AdminPostRetrieveCondition;
import com.seproject.admin.controller.dto.post.AdminPostResponse;
import com.seproject.admin.domain.repository.AdminPostSearchRepository;
import com.seproject.seboard.controller.PostSearchOptions;
import com.seproject.seboard.domain.model.category.QMenu;
import com.seproject.seboard.domain.model.common.Status;
import com.seproject.seboard.domain.model.post.Post;
import com.seproject.seboard.domain.model.post.QPost;
import com.seproject.seboard.domain.model.post.exposeOptions.ExposeOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;

import java.util.List;

import static com.seproject.seboard.domain.model.category.QCategory.category;
import static com.seproject.seboard.domain.model.category.QMenu.menu;
import static com.seproject.seboard.domain.model.post.QPost.post;

@Repository
public class AdminPostSearchJpaRepository implements AdminPostSearchRepository {
    private JPAQueryFactory queryFactory;

    @Autowired
    public AdminPostSearchJpaRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<AdminPostResponse> findPostListByCondition(AdminPostRetrieveCondition condition, Pageable pageable) {
        List<AdminPostResponse> contents = queryFactory
                .select(Projections.constructor(
                        AdminPostResponse.class,
                        post
                ))
                .from(post)
                .join(menu)
                .on(post.category.eq(menu))
                //TODO : ExposeOption 추가 필요?
                .where(
                        categoryEq(condition.getCategoryId()),
                        reportedStatusEq(condition.getIsReported()),
                        searchOption(condition.getSearchOption(), condition.getQuery())
                )
                .orderBy(post.baseTime.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = queryFactory
                .select(post.count())
                .from(post)
                .join(menu)
                .on(post.category.eq(menu))
                //TODO : ExposeOption 추가 필요?
                .where(
                        categoryEq(condition.getCategoryId()),
                        reportedStatusEq(condition.getIsReported()),
                        searchOption(condition.getSearchOption(), condition.getQuery())
                )
                .fetchOne();

        return new PageImpl<>(contents, pageable, count);
    }

    private BooleanExpression searchOption(String searchOption, String query){
        if(searchOption==null || query==null){
            return null;
        }

        BooleanExpression titleOption = post.title.contains(query);
        BooleanExpression contentOption = post.contents.contains(query);
        BooleanExpression authorOption = post.author.name.contains(query);

        switch (PostSearchOptions.valueOf(searchOption)){
            case TITLE:{
                return titleOption;
            }
            case CONTENT:{
                return contentOption;
            }
            case TITLE_OR_CONTENT:{
                return contentOption
                        .or(titleOption);
            }
            case AUTHOR:{
                return authorOption;
            }
            case ALL:{
                return titleOption
                        .or(contentOption)
                        .or(authorOption);
            }
            default:{
                return null;
            }
        }
    }

    private BooleanExpression categoryEq(Long categoryId){
        return categoryId != null ? menu.menuId.eq(categoryId).or(menu.superMenu.menuId.eq(categoryId)) : null;
    }

    private BooleanExpression reportedStatusEq(Boolean isReported){
        if(isReported == null){
            return post.status.eq(Status.NORMAL)
                    .or(post.status.eq(Status.REPORTED));
        }else if(!isReported){
            return post.status.eq(Status.NORMAL);
        } else{
            return post.status.eq(Status.REPORTED);
        }
    }
}
