package com.college.algorithm.service;

import com.college.algorithm.dto.*;
import com.college.algorithm.entity.*;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.mapper.AlgorithmMapper;
import com.college.algorithm.mapper.UserMapper;
import com.college.algorithm.repository.*;
import com.college.algorithm.util.AlgorithmSpecification;
import com.college.algorithm.util.CodeRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AlgorithmService {
    private final UserRepository userRepository;

    private final AlgorithmRepository algorithmRepository;
    private final AlgorithmImageRepository algorithmImageRepository;
    private final AlgorithmTestcaseRepository testcaseRepository;
    private final AlgorithmSuggestRepository suggestRepository;
    private final AlgorithmRecommendRepository recommendRepository;
    private final AlgorithmKindRepository kindRepository;
    private final AlgorithmCorrectRepository correctRepository;
    private final AlgorithmCorrectRecommendRepository correctRecommendRepository;
    private final ExplanationRepository explanationRepository;
    private final CommentRepository commentRepository;

    private final DummyImageRepository dummyImageRepository;
    private final BoardTypeRepository boardTypeRepository;
    private final BoardRepository boardRepository;
    private final BoardImageRepository boardImageRepository;
    private final TagRepository tagRepository;

    private final CodeTypeRepository codeTypeRepository;
    private final AlgorithmCorrectRepository algorithmCorrectRepository;
    private final TryRepository tryRepository;
    private final AlgorithmRecommendRepository algorithmRecommendRepository;

    public ResponseAlgorithmDto getAll(AlgorithmSearchRequestDto algorithmRequestDTO, Object loginUserId) {
        try {
            Pageable pageable = null;

            if ("r".equals(algorithmRequestDTO.getSort())) { // "r"이면 내림차순
                pageable = PageRequest.of(algorithmRequestDTO.getPage()-1, algorithmRequestDTO.getCount(), Sort.by("createdTime").descending());
            } else { // "or"이면 오름차순
                pageable = PageRequest.of(algorithmRequestDTO.getPage()-1, algorithmRequestDTO.getCount(), Sort.by("createdTime").ascending());
            }

            Page<Algorithm> algorithmEntities = algorithmRepository.findAll(
                    AlgorithmSpecification.withDynamicQuery(
                            algorithmRequestDTO.getLevel(),
                            algorithmRequestDTO.getTag(),
                            algorithmRequestDTO.getKeyword()),
                    pageable
            );
            Long total = algorithmEntities.getTotalElements();


            ObjectMapper objectMapper = new ObjectMapper();

            List<Algorithm> resultValue = algorithmEntities.getContent();

            ResponseAlgorithmDto response;
            List<AlgorithmDto> dtos = new ArrayList<>();

            for (Algorithm algorithm : resultValue) {
                // algorithmId로 testcase 찾아와서 node 생성
                List<AlgorithmTestcase> testcaseEntities = testcaseRepository.findTestcaseEntitiesByAlgorithmAlgorithmId(algorithm.getAlgorithmId());
                ArrayNode testcaseArrayNode = objectMapper.createArrayNode();
                if (testcaseEntities == null)
                    throw new CustomException(ErrorCode.NOT_FOUND_TESTCASE);
                for (AlgorithmTestcase testcaseEntity : testcaseEntities) {
                    AlgorithmTestcaseDto testcaseDTO = AlgorithmMapper.INSTANCE.toAlgorithmTestcaseDto(testcaseEntity);


                    ObjectNode testcaseNode = objectMapper.createObjectNode();
                    testcaseNode.put("testcaseId", testcaseDTO.getTestcaseId());
                    testcaseNode.put("algorithmId", testcaseDTO.getAlgorithm().getAlgorithmId());
                    testcaseNode.put("input", testcaseDTO.getInput());
                    testcaseNode.put("output", testcaseDTO.getOutput());
                    testcaseArrayNode.add(testcaseNode);
                }

                float tried = algorithmRepository.findTriedByAlgorithmId(algorithm.getAlgorithmId());
                float correct = algorithmRepository.findCorrectByAlgorithmId(algorithm.getAlgorithmId());
                float correctRate = 0.0f;
                if(tried > 0){
                    correctRate = correct / tried;
                    correctRate = Math.round(correctRate*100);
                }

                Boolean solved = null;
                if(loginUserId != null){
                    solved = correctRepository.countByAlgorithm_AlgorithmIdAndUser_UserId(algorithm.getAlgorithmId(), Long.parseLong(loginUserId.toString()))>=1;
                }

                dtos.add(AlgorithmMapper.INSTANCE.toAlgorithmDto(algorithm,correctRate,solved));
            }

            List<AlgorithmDto> list = new ArrayList<>();
            List<AlgorithmDto> sortedResponse = new ArrayList<>();

            if(Objects.equals(algorithmRequestDTO.getRate(), "h")) {
                list.addAll(dtos);

                // correctRate를 기준으로 내림차순 정렬
                list.sort(Comparator.comparing(AlgorithmDto::getCorrectRate).reversed());

                // 정렬된 데이터를 새로운 ArrayNode에 추가
                sortedResponse.addAll(list);
            } else if(Objects.equals(algorithmRequestDTO.getRate(), "l")) {
                list.addAll(dtos);

                // correctRate를 기준으로 오름차순 정렬
                list.sort(Comparator.comparing(AlgorithmDto::getCorrectRate));

                // 정렬된 데이터를 새로운 ArrayNode에 추가
                sortedResponse.addAll(list);
            } else {
                sortedResponse.addAll(dtos);
            }

            return new ResponseAlgorithmDto(sortedResponse,total);
        }
        catch (Error e){
            throw new CustomException(ErrorCode.SQL_EXCEPTION); // 잠깐만 이거임, SQLException 넣어야함.
        }
    }

    public ResponseAlgorithmSuggestDto getSuggestAlgorithms(){
        List<AlgorithmSuggest> algorithms = suggestRepository.findAll();

        ResponseAlgorithmSuggestDto response = new ResponseAlgorithmSuggestDto();
        List<AlgorithmSuggestDto> dtos = new ArrayList<>();

        for(AlgorithmSuggest algorithmSuggest : algorithms)
            dtos.add(AlgorithmMapper.INSTANCE.toAlgorithmSuggestDto(algorithmSuggest));

        response.setAlgorithms(dtos);

        return response;
    }
    public AlgorithmDetailDto getAlgorithmDetail(Long algorithmId, Object loginUserId){
        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));
        AlgorithmImage algorithmImage = algorithmImageRepository.findFirstByAlgorithmOrderByCreatedTimeAsc(algorithm);

        // algorithmId로 testcase 찾아와서 node 생성
        List<AlgorithmTestcase> testcaseEntities = testcaseRepository.findTestcaseEntitiesByAlgorithmAlgorithmId(algorithmId);
        List<AlgorithmDetailTestcaseDto> testcases = new ArrayList<>();
        if (testcaseEntities == null)
            throw new CustomException(ErrorCode.NOT_FOUND_TESTCASE);

        for (AlgorithmTestcase testcaseEntitiy : testcaseEntities) {
            AlgorithmTestcaseDto testcaseDTO = AlgorithmMapper.INSTANCE.toAlgorithmTestcaseDto(testcaseEntitiy);
            AlgorithmDetailTestcaseDto testcase = new AlgorithmDetailTestcaseDto();
            testcase.setInput(testcaseDTO.getInput());
            testcase.setOutput(testcaseDTO.getOutput());
            testcases.add(testcase);
        }

        // algorithmId 및 loginUserId로 추천 점검
        boolean isRecommend = false;
        if (loginUserId != null) {
            isRecommend = recommendRepository.existsByAlgorithmAndUser_UserId(algorithm, Long.parseLong(loginUserId.toString()));
        }
        return AlgorithmMapper.INSTANCE.toAlgorithmDetailDto(algorithm, algorithmImage, isRecommend, testcases);
    }
    public ExplanationResponseDto getExplanation(Long algorithmId){

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        Explanation explanation = explanationRepository.findExplanationByAlgorithm_AlgorithmId(algorithmId);
        if(explanation == null)
            throw new CustomException(ErrorCode.NOT_FOUND_EXPLANATION);

        return AlgorithmMapper.INSTANCE.toExplanationResponseDto(algorithm,explanation);
    }
    public ResponseAlgorithmKindDto getKinds(){

        List<AlgorithmKind> kinds = kindRepository.findAll();
        ResponseAlgorithmKindDto response = new ResponseAlgorithmKindDto();
        List<AlgorithmKindDto> kindDtos = new ArrayList<>();

        for(AlgorithmKind kind : kinds){
            AlgorithmKindDto kindDto = new AlgorithmKindDto();
            kindDto.setKindId(Integer.parseInt(kind.getKindId()));
            kindDto.setKindName(kind.getKindName());
            kindDtos.add(kindDto);
        }
        response.setKinds(kindDtos);

        return response;
    }
    public ResponseCorrectDto getCorrects(Long algorithmId, int page, int count, String codeType){
        Pageable pageable = PageRequest.of(page-1, count, Sort.by("createdTime").descending());

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        Page<AlgorithmCorrect> corrects = correctRepository.findAllByAlgorithmAndCodeType_TypeIdOrderByCreatedTimeDesc(pageable, algorithm, codeType);

        List<AlgorithmCorrectDto> dtos = new ArrayList<>();
        long total = corrects.getTotalElements();

        for(AlgorithmCorrect correct : corrects) {
            ResponseAlgorithmUserDto user = UserMapper.INSTANCE.toResponseAlgorithmUserDto(correct.getUser());
            dtos.add(AlgorithmMapper.INSTANCE.toAlgorithmCorrectDto(correct, user));
        }

        return new ResponseCorrectDto(dtos, total);
    }
    public ResponseCorrectCommentDto getCorrectComments(Long correctId, int page, int count){
        Pageable pageable = PageRequest.of(page-1, count, Sort.by("createdTime").ascending());

        if(correctRepository.findByCorrectId(correctId) == null)
            throw new CustomException(ErrorCode.NOT_FOUND_CORRECT);

        Page<Comment> comments = commentRepository.findAllByCorrectCorrectIdOrderByCreatedTimeDesc(pageable, correctId);

        List<CorrectCommentDto> dtos = new ArrayList<>();

        long total = comments.getTotalElements();
        for(Comment comment : comments) {
            ResponseAlgorithmUserDto user = UserMapper.INSTANCE.toResponseAlgorithmUserDto(comment.getUser());
            dtos.add(AlgorithmMapper.INSTANCE.toCorrectCommentDto(comment,user));
        }

        return new ResponseCorrectCommentDto(dtos, total);
    }
    public ResponsePostCodeDto postCode(RequestCodeDto codeDto, Long algorithmId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        CodeRunner codeRunner = new CodeRunner(testcaseRepository);
        ResponseCodeDto codeResponseDTO = null;

        try {
            codeResponseDTO = switch (codeDto.getType()) {
                case "3001" -> codeRunner.runCpp(codeDto, algorithmId);
                case "3002" -> codeRunner.runPython(codeDto, algorithmId);
                case "3003" -> codeRunner.runJava(codeDto, algorithmId);
                default -> codeResponseDTO;
            };
        }
        catch (IOException | InterruptedException e) {
            throw new CustomException(ErrorCode.ERROR_CODE_RUNNER);
        }

        if (codeResponseDTO == null) {
            throw new CustomException(ErrorCode.ERROR_CODE_RUNNER);
        }

        Boolean solved = codeResponseDTO.getSolved() && (Double.parseDouble(algorithm.getLimitTime()) > codeResponseDTO.getExcuteTime());

        AlgorithmCorrect postCode = new AlgorithmCorrect();
        postCode.setUser(user);
        postCode.setAlgorithm(algorithm);
        postCode.setCodeType(codeTypeRepository.findCodeTypeByTypeId(codeDto.getType()));
        postCode.setCode(codeDto.getCode());

        if(solved){
            List<AlgorithmCorrect> algorithmCorrect = algorithmCorrectRepository.findAllByAlgorithmAndUser(algorithm, user);
            if (algorithmCorrect == null || algorithmCorrect.isEmpty()) {
                user.setSolved(user.getSolved() + 1);
            }

            algorithmCorrectRepository.save(postCode);
            Try codeTry = new Try(user,algorithm,solved,String.valueOf(codeResponseDTO.getExcuteTime()),"0");
            tryRepository.save(codeTry);
        } else{
            Try codeTry = new Try(user,algorithm,solved,String.valueOf(codeResponseDTO.getExcuteTime()),"0");
            tryRepository.save(codeTry);
        }

        user.setTried(user.getTried() + 1);

        ResponsePostCodeDto response = new ResponsePostCodeDto();
        response.setIsSuccess(solved);
        response.setUseTime(String.valueOf(codeResponseDTO.getExcuteTime()));

        userRepository.save(user);

        // codeResponseDTO
        return response;
    }
    public HttpStatus postCorrectComment(Long correctId, RequestCorrectComment requestCorrectComment, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        AlgorithmCorrect correct = correctRepository.findByCorrectId(correctId);
        if(correct == null)
            throw new CustomException(ErrorCode.NOT_FOUND_CORRECT);

        Comment comment = new Comment(user, null, correct, requestCorrectComment.getContent());

        commentRepository.save(comment);

        return HttpStatus.CREATED;
    }
    public HttpStatus postCorrectRecommend(Long correctId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        AlgorithmCorrect correct = correctRepository.findByCorrectId(correctId);
        if(correct == null)
            throw new CustomException(ErrorCode.NOT_FOUND_CORRECT);

        if(correctRecommendRepository.findByCorrect_CorrectIdAndUser_UserId(correctId, loginUserId) != null)
            throw new CustomException(ErrorCode.DUPLICATE_RECOMMEND);


        AlgorithmCorrectRecommend recommend = new AlgorithmCorrectRecommend(user, correct);

        correctRecommendRepository.save(recommend);
        correct.setRecommendCount(correctRecommendRepository.countByCorrect_CorrectId(correctId));
        correctRepository.save(correct);

        return HttpStatus.CREATED;
    }


    public HttpStatus postAlgorithmRecommend(Long algorithmId, Long loginUserId){
        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        if(algorithmRecommendRepository.countByAlgorithm_AlgorithmIdAndUserUserId(algorithmId, loginUserId) >= 1)
            throw new CustomException(ErrorCode.DUPLICATE_RECOMMEND);


        AlgorithmRecommend recommend = new AlgorithmRecommend(user, algorithm);

        algorithmRecommendRepository.save(recommend);
        algorithm.setRecommendCount(algorithmRecommendRepository.countByAlgorithm_AlgorithmId(algorithmId));
        algorithmRepository.save(algorithm);

        return HttpStatus.CREATED;
    }

    public HttpStatus postAlgorithmBoard(RequestAlgorithmPostDto boardPostDto,Long algorithmId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        List<Long> dummyImageIds = boardPostDto.getImageIds();
        List<DummyImage> dummyImages = new ArrayList<>();

        if (dummyImageIds != null) {
            for(Long imageId : dummyImageIds){
                DummyImage image = dummyImageRepository.findDummyImageByImageId(imageId);
                if(image != null)
                    dummyImages.add(image);
                else
                    throw new CustomException(ErrorCode.NOT_FOUND_IMAGE);
            }
        }

        Board board = Board.builder()
                .user(user)
                .title(boardPostDto.getTitle())
                .content(boardPostDto.getContent())
                .boardType(boardTypeRepository.findBoardTypeByTypeId(Character.forDigit(boardPostDto.getBoardType(),10)))
                .algorithm(algorithm)
                .build();
        Board savedBoard = boardRepository.save(board);

        List<BoardImage> addImages = new ArrayList<>();
        for(DummyImage image : dummyImages){
            addImages.add(BoardImage.builder()
                    .imageId(image.getImageId())
                    .board(savedBoard)
                    .imageType(image.getImageType())
                    .imageSize(image.getImageSize())
                    .imagePath(image.getImagePath())
                    .build());
        }

        boardImageRepository.saveAll(addImages);
        dummyImageRepository.deleteAll(dummyImages);

        List<String> tagNames = boardPostDto.getTags();

        if (tagNames != null && !tagNames.isEmpty()) {
            List<Tag> tags = new ArrayList<>();
            for (String tagName : tagNames) {
                tags.add(new Tag(savedBoard, tagName));
            }

            tagRepository.saveAll(tags);
        }

        return HttpStatus.OK;
    }

    public HttpStatus deleteCorrectRecommend(Long correctId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        AlgorithmCorrect correct = correctRepository.findByCorrectId(correctId);
        if(correct == null)
            throw new CustomException(ErrorCode.NOT_FOUND_CORRECT);

        AlgorithmCorrectRecommend recommend = correctRecommendRepository.findByCorrect_CorrectIdAndUser_UserId(correctId, user.getUserId());

        if(recommend == null)
            throw new CustomException(ErrorCode.NOT_FOUND_RECOMMEND);

        if(!recommend.getUser().getUserId().equals(loginUserId))
            throw new CustomException(ErrorCode.NOT_MATCHED_USER);

        correctRecommendRepository.delete(recommend);
        correct.setRecommendCount(correctRecommendRepository.countByCorrect_CorrectId(correctId));
        correctRepository.save(correct);

        return HttpStatus.OK;
    }


    public HttpStatus deleteAlgorithmRecommend(Long algorithmId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Algorithm algorithm = algorithmRepository.findAlgorithmByAlgorithmId(algorithmId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ALGORITHM));

        AlgorithmRecommend recommend = algorithmRecommendRepository.findByAlgorithm_AlgorithmIdAndUser_UserId(algorithmId, user.getUserId());

        if(recommend == null)
            throw new CustomException(ErrorCode.NOT_FOUND_RECOMMEND);

        if(!recommend.getUser().getUserId().equals(loginUserId))
            throw new CustomException(ErrorCode.NOT_MATCHED_USER);

        algorithmRecommendRepository.delete(recommend);
        algorithm.setRecommendCount(algorithmRecommendRepository.countByAlgorithm_AlgorithmId(algorithmId));
        algorithmRepository.save(algorithm);

        return HttpStatus.OK;
    }
}
