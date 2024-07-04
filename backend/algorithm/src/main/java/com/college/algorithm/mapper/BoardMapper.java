package com.college.algorithm.mapper;

import com.college.algorithm.dto.*;
import com.college.algorithm.entity.AppUser;
import com.college.algorithm.entity.Board;
import com.college.algorithm.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = CustomTimestampMapper.class)
public interface BoardMapper {
    BoardMapper INSTANCE = Mappers.getMapper(BoardMapper.class);

    @Mapping(source = "board.boardId", target = "boardId")
    @Mapping(source = "board.boardType.typeName", target = "boardType")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "board.title", target = "title")
    @Mapping(source = "board.viewCount", target = "viewCount")
    @Mapping(source = "board.recommendCount", target = "recommendCount")
    @Mapping(source = "board.commentCount", target = "commentCount")
    @Mapping(source = "tags", target = "tags")
    @Mapping(source = "isSolved", target = "isSolved")
    @Mapping(source = "isRecommend", target = "isRecommend")
    @Mapping(source = "board.createdTime", target = "createdTime", qualifiedBy = { CustomTimestampTranslator.class, MapCreatedTime.class})
    BoardDto toBoardDto(Board board, ResponseBoardUserDto user, List<String> tags, Boolean isSolved, Boolean isRecommend);

    @Mapping(source = "board.boardId", target = "boardId")
    @Mapping(source = "board.boardType.typeName", target = "boardType")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "board.title", target = "title")
    @Mapping(source = "board.content", target = "content")
    @Mapping(source = "board.viewCount", target = "viewCount")
    @Mapping(source = "board.recommendCount", target = "recommendCount")
    @Mapping(source = "board.commentCount", target = "commentCount")
    @Mapping(source = "tags", target = "tags")
    @Mapping(source = "isSolved", target = "isSolved")
    @Mapping(source = "isView", target = "isView")
    @Mapping(source = "isRecommend", target = "isRecommend")
    @Mapping(source = "board.createdTime", target = "createdTime", qualifiedBy = { CustomTimestampTranslator.class, MapCreatedTime.class})
    ResponseBoardDetailDto toResponseBoardDetailDto(Board board, ResponseBoardUserDto user, List<String> tags, Boolean isSolved, Boolean isView, Boolean isRecommend);

    @Mapping(source = "board.boardId", target = "boardId")
    @Mapping(source = "board.boardType.typeName", target = "boardType")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "board.title", target = "title")
    @Mapping(source = "tags", target = "tags")
    BoardSuggestDto toBoardSuggestDto(Board board, ResponseBoardUserDto user,List<String> tags);

    @Mapping(source = "comment.commentId", target = "commentId")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "comment.content", target = "content")
    @Mapping(source = "comment.recommendCount", target = "recommendCount")
    @Mapping(source = "comment.createdTime", target = "createdTime", qualifiedBy = { CustomTimestampTranslator.class, MapCreatedTime.class})
    BoardCommentDto toBoardCommentDto(Comment comment, ResponseBoardUserDto user);
}
