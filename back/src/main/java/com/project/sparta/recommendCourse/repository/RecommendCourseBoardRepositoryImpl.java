package com.project.sparta.recommendCourse.repository;


import static com.project.sparta.communityBoard.entity.QCommunityBoard.communityBoard;

import com.project.sparta.communityBoard.dto.CommunitySearchCondition;
import com.project.sparta.like.entity.CourseLike;
import com.project.sparta.like.entity.QCourseLike;
import com.project.sparta.recommendCourse.dto.RecommendCondition;
import com.project.sparta.recommendCourse.dto.RecommendDetailResponseDto;
import com.project.sparta.recommendCourse.dto.RecommendResponseDto;
import com.project.sparta.recommendCourse.entity.PostStatusEnum;
import com.project.sparta.recommendCourse.entity.QRecommendCourseBoard;
import com.project.sparta.recommendCourse.entity.QRecommendCourseImg;
import com.project.sparta.recommendCourse.entity.QRecommendCourseThumbnail;
import com.project.sparta.recommendCourse.entity.RecommendCourseBoard;
import com.project.sparta.recommendCourse.entity.RecommendCourseImg;
import com.project.sparta.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.swagger.models.auth.In;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class RecommendCourseBoardRepositoryImpl implements RecommendCourseBoardCustomRepository {

    private final JPAQueryFactory queryFactory;

    QUser user = new QUser("user");

    QRecommendCourseBoard reBoard = new QRecommendCourseBoard("reBoard");

    QCourseLike cLike = new QCourseLike("cLike");

    QRecommendCourseImg courseImg = new QRecommendCourseImg("courseImg");

    QRecommendCourseThumbnail thumbnail = new QRecommendCourseThumbnail("thumbnail");

    //코스 전체 조회(+필터링)
    @Override
    public Page<RecommendResponseDto> allRecommendBoardList(PageRequest pageable,
        PostStatusEnum postStatusEnum, RecommendCondition condition) {

        List<RecommendResponseDto> recommendBoards = new ArrayList<>();
        StringPath likeOrderBy = Expressions.stringPath("likeCount");
        StringPath dateOrderBy = Expressions.stringPath("dateOrderBy");

        OrderSpecifier[] orderSpecifiers = createOrderSpecifier(condition.getOrderByLike(), likeOrderBy, dateOrderBy);

        QueryResults<Tuple> results = queryFactory
            .select(
                reBoard.id,
                reBoard.title,
                thumbnail.url,
                ExpressionUtils.as(JPAExpressions.select(cLike.courseBoard.count()).from(cLike)
                    .where(cLike.courseBoard.id.eq(reBoard.id)), "likeCount"),
                user.nickName,
                reBoard.modifiedAt.as("dateOrderBy"))
            .from(reBoard)
            .leftJoin(user).on(reBoard.userId.eq(user.Id))
            .leftJoin(thumbnail).on(reBoard.id.eq(thumbnail.recommendCourseBoard.id))
            .where(reBoard.postStatus.eq(postStatusEnum),
                regionEq(condition.getRegion()),
                scoreEq(condition.getScore()),
                seasonEq(condition.getSeason()),
                altitudeEq(condition.getAltitude()))
            .orderBy(orderSpecifiers)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();

        List<Tuple> boards = results.getResults();
        long total = results.getTotal();

        for(Tuple t : boards) {
            Long Id = t.get(reBoard.id);
            String title = t.get(reBoard.title);
            String thumbnail_url = t.get(thumbnail.url);
            String nickName = t.get(user.nickName);
            LocalDateTime createDate = t.get(reBoard.modifiedAt.as("dateOrderBy"));
            Long like = t.get(ExpressionUtils.as(JPAExpressions.select(cLike.courseBoard.count()).from(cLike)
                .where(cLike.courseBoard.id.eq(reBoard.id)), "likeCount"));

            //이미지 리스트 뽑기
            List<String> imgList = queryFactory.select(courseImg.url)
                .from(courseImg)
                .where(courseImg.recommendCourseBoard.id.eq(Id)).fetch();

            recommendBoards.add(new RecommendResponseDto(Id, title, thumbnail_url, imgList, createDate, like, nickName));
        }

        return new PageImpl<>(recommendBoards, pageable, total);
    }

    //코스 단건 조회
    @Override
    public RecommendDetailResponseDto getCourseBoard(Long boardId, PostStatusEnum postStatusEnum, String loginNickName) {

        Tuple result = queryFactory.select(
                reBoard.id,
                reBoard.score,
                reBoard.title,
                reBoard.season,
                reBoard.altitude,
                reBoard.contents,
                reBoard.region,
                reBoard.modifiedAt,
                user.nickName,
                JPAExpressions.select(cLike.userNickName.count().when(1L).then(true)
                        .otherwise(false))
                    .from(cLike)
                    .where(
                        cLike.courseBoard.id.eq(boardId),
                        cLike.userNickName.eq(loginNickName)))
            .from(reBoard)
            .leftJoin(user).on(reBoard.userId.eq(user.Id))
            .where(reBoard.postStatus.eq(postStatusEnum), reBoard.id.eq(boardId))
            .fetchOne();

        Long id = result.get(reBoard.id);
        int score = result.get(reBoard.score);
        String title = result.get(reBoard.title);
        String season = result.get(reBoard.season);
        int altitude = result.get(reBoard.altitude);
        String contents = result.get(reBoard.contents);
        String region = result.get(reBoard.region);
        LocalDateTime modifiedAt = result.get(reBoard.modifiedAt);
        String nickName = result.get(user.nickName);

        List<String> images = queryFactory.select(courseImg.url)
            .from(courseImg)
            .join(courseImg.recommendCourseBoard, reBoard)
            .where(courseImg.recommendCourseBoard.id.eq(boardId))
            .fetch();

        Long like = queryFactory.select(cLike.courseBoard.count())
                                                .from(cLike)
                                                .where(cLike.courseBoard.id.eq(boardId))
                                                .fetchOne();
        Boolean isLike = result.get(
            JPAExpressions.select(cLike.userNickName.count().when(1L).then(true)
                    .otherwise(false))
                .from(cLike)
                .where(
                    cLike.courseBoard.id.eq(boardId),
                    cLike.userNickName.eq(loginNickName)
                ));

        RecommendDetailResponseDto recommendDto = RecommendDetailResponseDto.builder()
            .boardId(id)
            .score(score)
            .title(title)
            .season(season)
            .altitude(altitude)
            .contents(contents)
            .region(region)
            .createDate(modifiedAt)
            .likeCount(like)
            .nickName(nickName)
            .imgList(images)
            .isLike(isLike)
            .build();

        return recommendDto;
    }

    // 나의 코스추천 글 조회
    @Override
    public Page<RecommendResponseDto> myRecommendBoardList(PageRequest pageable,
        PostStatusEnum postStatusEnum, Long userId) {
        List<RecommendResponseDto> recommendBoards = new ArrayList<>();
        QueryResults<Tuple> results = queryFactory
            .select(
                reBoard.id,
                reBoard.title,
                thumbnail.url,
                ExpressionUtils.as(JPAExpressions.select(cLike.courseBoard.count()).from(cLike)
                    .where(cLike.courseBoard.id.eq(reBoard.id)), "likeCount"),
                user.nickName,
                reBoard.modifiedAt)
            .from(reBoard)
            .leftJoin(user).on(reBoard.userId.eq(user.Id))
            .leftJoin(thumbnail).on(reBoard.id.eq(thumbnail.recommendCourseBoard.id))
            .where(reBoard.userId.eq(userId))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();
        List<Tuple> boards = results.getResults();
        long total = results.getTotal();

        for(Tuple t : boards) {
            Long Id = t.get(reBoard.id);
            String title = t.get(reBoard.title);
            String thumbnail_url = t.get(thumbnail.url);
            String nickName = t.get(user.nickName);
            LocalDateTime createDate = t.get(reBoard.modifiedAt);
            Long like = t.get(ExpressionUtils.as(JPAExpressions.select(cLike.courseBoard.count()).from(cLike)
                .where(cLike.courseBoard.id.eq(reBoard.id)), "likeCount"));

            //이미지 리스트 뽑기
            List<String> imgList = queryFactory.select(courseImg.url)
                .from(courseImg)
                .where(courseImg.recommendCourseBoard.id.eq(Id)).fetch();

            recommendBoards.add(new RecommendResponseDto(Id, title, thumbnail_url, imgList, createDate, like, nickName));
        }
        return new PageImpl<>(recommendBoards, pageable, total);
    }

    //좋아요 필터링
    private OrderSpecifier[] createOrderSpecifier(String orderByLike, StringPath likeOrderBy,
        StringPath dateOrderBy) {

        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();

        if (orderByLike.equals("likeDesc")) {
            orderSpecifiers.add(new OrderSpecifier(Order.DESC, likeOrderBy));
        } else if (orderByLike.equals("dateDesc")) {
            orderSpecifiers.add(new OrderSpecifier(Order.DESC, dateOrderBy));
        }
        return orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]);
    }

    private Predicate regionEq(String region) {
        return region != "all" ?  reBoard.region.contains(region) : null;
    }
    private Predicate scoreEq(int score) {
        return score != 0 ? reBoard.score.eq(score) : null;
    }
    private Predicate seasonEq(String season) {
        return season != "all" ? reBoard.season.contains(season) : null;
    }
    private Predicate altitudeEq(int altitude) {
        return altitude != 0 ? reBoard.altitude.between(altitude, altitude+100): null;
    }
}

