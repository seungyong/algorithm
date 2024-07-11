package com.college.algorithm.controller;

import com.college.algorithm.dto.RequestBoardUpdateDto;
import com.college.algorithm.dto.RequestCommentUpdateDto;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ResponseBody
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{comment_id}/recommend")
    public ResponseEntity<?> postCommentRecommend(@PathVariable(value = "comment_id") Long commentId,
                                                           HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(commentService.postCommentRecommend(commentId,loginUserId)).build();
    }
    @PatchMapping("/{comment_id}")
    public ResponseEntity<?> patchBoard(@RequestBody(required = false) RequestCommentUpdateDto commentUpdateDto,
                                        @PathVariable(value = "comment_id") Long commentId,
                                        HttpServletRequest request){

//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(HttpStatus.OK).body(commentService.patchComment(commentUpdateDto,commentId,loginUserId));
    }

    @DeleteMapping("/{comment_id}/recommend")
    public ResponseEntity<?> deleteCommentRecommend(@PathVariable(value = "comment_id") Long commentId,
                                                             HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(commentService.deleteCommentRecommend(commentId,loginUserId)).build();
    }

    @DeleteMapping("/{comment_id}")
    public ResponseEntity<?> deleteComment(@PathVariable(value = "comment_id") Long commentId,
                                                             HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(commentService.deleteComment(commentId,loginUserId)).build();
    }

}