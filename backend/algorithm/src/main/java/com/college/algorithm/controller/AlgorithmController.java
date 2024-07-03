package com.college.algorithm.controller;

import com.college.algorithm.dto.*;
import com.college.algorithm.exception.BadRequestException;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.service.AlgorithmService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@ResponseBody
@RequiredArgsConstructor
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    @GetMapping("/algorithm")
    public ResponseEntity<?> getAllAlgorithm(@RequestParam(required = false, defaultValue = "1", value = "page") int page,
                                             @RequestParam(required = false, defaultValue = "10", value = "count") int count,
                                             @RequestParam(required = false, defaultValue = "a", value = "solved") String solved, // a : 전체, s : 푼 문제, ns : 안푼 문제
                                             @RequestParam(required = false, defaultValue = "r", value = "sort") String sort, // r : 최신순, or : 오래된순, t : 시도순
                                             @RequestParam(required = false, defaultValue = "-1", value = "level") String level, // -1은 전체, 0~5
                                             @RequestParam(required = false, value = "rate") String rate, // h : 정답률 높은 순, l = 정답률 낮은 순
                                             @RequestParam(required = false, value = "tag") String tag, // 1~14
                                             @RequestParam(required = false, value = "keyword") String keyword,
                                             HttpServletRequest request) {
        Set<String> validLevel = new HashSet<>(Arrays.asList("-1","0","1","2","3","4","5"));
        if (!validLevel.contains(level)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST_SEARCH);
        }
        Set<String> validSort = new HashSet<>(Arrays.asList("r","or","t","R","OR","T"));
        if (!validSort.contains(sort)) {
//            return ResponseEntity.badRequest().body(
//                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request : sort ( sort only 'r', 'or', 't' : default 'r' )")
//            );
            throw new BadRequestException(ErrorCode.BAD_REQUEST_SEARCH);
        }

        Set<String> validSolved = new HashSet<>(Arrays.asList("a","s","ns","A","S","NS"));
        if (!validSolved.contains(solved)) {
//            return ResponseEntity.badRequest().body(
//                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request : solved ( solved only 'a', 's', 'ns' : default 'a' )")
//            );

            throw new BadRequestException(ErrorCode.BAD_REQUEST_SEARCH);
        }

        Set<String> validRate = new HashSet<>(Arrays.asList("h","l","H","L"));
        if (!validRate.contains(rate) && rate != null) {
//            return ResponseEntity.badRequest().body(
//                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request : rate ( rate only 'h', 'l' )")
//            );

            throw new BadRequestException(ErrorCode.BAD_REQUEST_SEARCH);
        } else if (rate != null){
            rate = rate.toLowerCase();
        }

        Set<String> validTag = new HashSet<>(Arrays.asList("1001","1002","1003","1004","1005","1006","1007","1008","1009","1010","1011","1012","1013","1014"));
        if (!validTag.contains(tag) && tag != null) {
//            return ResponseEntity.badRequest().body(
//                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request : tag ( tag range 1001 ~ 1014 )" + " input : ")
//            );

            throw new BadRequestException(ErrorCode.BAD_REQUEST_SEARCH);
        }


        HttpSession session = request.getSession(false); // default true
        AlgorithmSearchRequestDto algorithmRequestDTO = new AlgorithmSearchRequestDto(page,count,solved.toLowerCase(),sort.toLowerCase(),level,rate,tag,keyword);
        Long loginUser = null; // String이 아니라 UserDTO로 받아야하고, 밑에서 세션으로 가져와야만합니다.
        if(session != null){
            // 상수로 뺼 예정
            loginUser = (Long)session.getAttribute("로그인 암호 키 무언가");
        }

        if (loginUser != null) {
            return ResponseEntity.status(HttpStatus.OK).body(algorithmService.getAll(algorithmRequestDTO, loginUser));
        } else {
            // 세션에 loginUser가 없으면 로그인되지 않은 상태
            return ResponseEntity.status(HttpStatus.OK).body(algorithmService.getAll(algorithmRequestDTO));
        }
    }

    @GetMapping("/algorithm/recommend")
    public ResponseEntity<?> getSuggestAlgorithm() {
        AlgorithmSuggestResponseDto response = algorithmService.getSuggestAlgorithms();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/algorithm/{algorithm_id}")
    public ResponseEntity<?> getAlgorithmDetail(@PathVariable(value = "algorithm_id") Long algorithm_id,
                                                HttpServletRequest request) {
        Long loginUserId = (Long) request.getAttribute("유저유저키키");
        AlgorithmDetailDto response = algorithmService.getAlgorithmDetail(algorithm_id,1L);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/algorithm/{algorithm_id}/explanation")
    public ResponseEntity<?> getExplanation(@PathVariable(value = "algorithm_id") Long algorithm_id,
                                            HttpServletRequest request) {
        Long loginUserId = (Long) request.getAttribute("유저유저키키");
        ExplanationResponseDto response = algorithmService.getExplanation(algorithm_id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/algorithm/kind")
    public ResponseEntity<?> getKinds() {
        AlgorithmKindResponseDto response = algorithmService.getKinds();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/algorithm/{algorithm_id}/correct")
    public ResponseEntity<?> getAlgorithmCorrect(@RequestParam(required = false, defaultValue = "1", value = "page") int page,
                                                 @RequestParam(required = false, defaultValue = "10", value = "count") int count,
                                                 @PathVariable(value = "algorithm_id") Long algorithm_id) {
        ResponseCorrectDto response = algorithmService.getCorrects(algorithm_id,page,count);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/algorithm/{algorithm_id}/correct/{correct_id}/comment")
    public ResponseEntity<?> getAlgorithmCorrectComment(@RequestParam(required = false, defaultValue = "1", value = "page") int page,
                                                 @RequestParam(required = false, defaultValue = "10", value = "count") int count,
                                                 @PathVariable(value = "algorithm_id") Long algorithm_id,
                                                    @PathVariable(value = "correct_id") Long correct_id) {
        ResponseCorrectCommentDto response = algorithmService.getCorrectComments(correct_id,page,count);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/algorithm/{algorithm_id}/correct/{correct_id}/comment")
    public ResponseEntity<?> postAlgorithmCorrectComment(@RequestBody(required = false) RequestCorrectComment requestCorrectComment,
                                                        @PathVariable(value = "algorithm_id") Long algorithm_id,
                                                        @PathVariable(value = "correct_id") Long correct_id,
                                                        HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(algorithmService.postCorrectComment(correct_id,requestCorrectComment,loginUserId)).build();
    }

    @PostMapping("/algorithm/{algorithm_id}/correct/{correct_id}/recommend")
    public ResponseEntity<?> postAlgorithmCorrectRecommend(@PathVariable(value = "algorithm_id") Long algorithm_id,
                                                         @PathVariable(value = "correct_id") Long correct_id,
                                                         HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(algorithmService.postCorrectRecommend(correct_id,loginUserId)).build();
    }

    @DeleteMapping("/algorithm/{algorithm_id}/correct/{correct_id}/recommend")
    public ResponseEntity<?> deleteAlgorithmCorrectRecommend(@PathVariable(value = "algorithm_id") Long algorithm_id,
                                                           @PathVariable(value = "correct_id") Long correct_id,
                                                           HttpServletRequest request) {
//        Long loginUserId = (Long) request.getAttribute("로그인 키키키키키");
        Long loginUserId = 1L;
        if(loginUserId == null)
            throw new CustomException(ErrorCode.INVALID_COOKIE);

        return ResponseEntity.status(algorithmService.deleteCorrectRecommend(correct_id,loginUserId)).build();
    }

}
