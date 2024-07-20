package com.college.algorithm.service;

import com.college.algorithm.dto.*;
import com.college.algorithm.entity.*;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.mapper.BoardMapper;
import com.college.algorithm.mapper.UserMapper;
import com.college.algorithm.repository.*;
import com.college.algorithm.util.FileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardTypeRepository kindRepository;
    private final BoardRepository boardRepository;
    private final BoardRecommendRepository recommendRepository;
    private final BoardViewRepository boardViewRepository;
    private final BoardSuggestRepository suggestRepository;

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    private final FileStore fileStore;
    private final DummyImageRepository dummyImageRepository;
    private final BoardImageRepository boardImageRepository;

    public ResponseBoardsDto getBoards(int page, int count, String searchType, String keyword, Long loginUserId, Long algorithmId){
        Pageable pageable = PageRequest.of(page-1, count);
        Page<Board> boards = null;
        Map<String, String[]> searchTypeMappings = Map.of(
                "p", new String[] {"일반 질문", "일반 자유"},
                "pq", new String[] {"일반 질문"},
                "pf", new String[] {"일반 자유"},
                "a", new String[] {"알고리즘 질문", "알고리즘 피드백"},
                "aq", new String[] {"알고리즘 질문"},
                "af", new String[] {"알고리즘 피드백"}
        );
        List<BoardType> boardTypes = new ArrayList<>();
        String[] typeNames = searchTypeMappings.get(searchType);

        if (typeNames != null) {
            for (String typeName : typeNames) {
                boardTypes.add(kindRepository.findBoardTypeByTypeName(typeName));
            }
        }

        List<Character> typeIds = boardTypes.stream()
                .map(BoardType::getTypeId)
                .toList();

        if (searchType.equals("a") || searchType.equals("aq") || searchType.equals("af")) {
            if (algorithmId == null) {
                boards = boardRepository.findAllByBoardType_TypeIdInAndTitleContaining(pageable, typeIds, keyword);
            } else {
                boards = boardRepository.findAllByBoardType_TypeIdInAndTitleContainingAndAlgorithm_AlgorithmId(pageable, typeIds, keyword, algorithmId);
            }
        } else {
            boards = boardRepository.findAllByBoardType_TypeIdInAndTitleContaining(pageable, typeIds, keyword);
        }

        List<BoardDto> dtos = new ArrayList<>();
        List<Tag> tags = tagRepository.findAllByBoardIn(boards.getContent().stream().toList());
        long total = boards.getTotalElements();

        for(Board board : boards){
            ResponseBoardUserDto user = UserMapper.INSTANCE.toResponseBoardUserDto(board.getUser());

            Boolean isSolved = loginUserId != null && board.getAdopt() != null;

            Boolean isRecommend = loginUserId != null && board.getRecommendCount() > 0 && (
                    recommendRepository.existsByBoard_BoardIdAndUserUserId(board.getBoardId(), loginUserId)
            );

            List<Tag> boardTags = tags.stream()
                    .filter(tag -> tag.getBoard().getBoardId().equals(board.getBoardId()))
                    .toList();
            List<String> tagNames = new ArrayList<>();

            for(Tag tag : boardTags)
                tagNames.add(tag.getContent());

            if(tagNames.isEmpty())
                tagNames = null;

            dtos.add(BoardMapper.INSTANCE.toBoardDto(board, user, tagNames, isSolved, isRecommend));
        }

        return new ResponseBoardsDto(dtos, total);
    }
    public ResponseBoardTypeDto getKinds(){

        List<BoardType> types = kindRepository.findAll();
        ResponseBoardTypeDto response = new ResponseBoardTypeDto();
        List<BoardTypeDto> dtos = new ArrayList<>();

        for(BoardType type : types){
            BoardTypeDto typeDto = new BoardTypeDto(Integer.parseInt(String.valueOf(type.getTypeId())),type.getTypeName());
            dtos.add(typeDto);
        }
        response.setTypes(dtos);

        return response;
    }
    public ResponseBoardDetailDto getBoardDetail(Long boardId, Long loginUserId){
        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        ResponseBoardUserDto user = UserMapper.INSTANCE.toResponseBoardUserDto(board.getUser());

        Boolean isSolved = loginUserId != null && board.getAdopt() != null;

        boolean isView = loginUserId != null && (
                boardViewRepository.existsByBoardAndUserUserId(board, loginUserId)
        );
        if(loginUserId != null)
            isView = boardViewRepository.countByBoard_BoardIdAndUserUserId(board.getBoardId(), loginUserId) >= 1;

        Boolean isRecommend = loginUserId != null && (
                recommendRepository.existsByBoard_BoardIdAndUserUserId(board.getBoardId(), loginUserId)
        );

        List<Tag> tags = tagRepository.findAllByBoard_BoardId(board.getBoardId());
        List<String> tagNames = new ArrayList<>();
        for(Tag tag : tags)
            tagNames.add(tag.getContent());
        if(tagNames.isEmpty())
            tagNames = null;


        return BoardMapper.INSTANCE.toResponseBoardDetailDto(board,user,tagNames,isSolved,isView,isRecommend);
    }
    public ResponseBoardSuggestDto getSuggestBoards(){
        List<BoardSuggest> boards = suggestRepository.findAllByOrderByCreatedTimeDesc();
        List<BoardSuggestDto> dtos = new ArrayList<>();

        for(BoardSuggest boardSuggest : boards) {
            ResponseBoardUserDto user = UserMapper.INSTANCE.toResponseBoardUserDto(boardSuggest.getBoard().getUser());
            List<Tag> tags = tagRepository.findAllByBoard_BoardId(boardSuggest.getBoard().getBoardId());
            List<String> tagNames = new ArrayList<>();
            for(Tag tag : tags)
                tagNames.add(tag.getContent());
            if(tagNames.isEmpty())
                tagNames = null;

            BoardImage image = boardImageRepository.findByBoardOrderByCreatedTimeAsc(boardSuggest.getBoard());
            dtos.add(BoardMapper.INSTANCE.toBoardSuggestDto(boardSuggest.getBoard(), image, user, tagNames));
        }


        return new ResponseBoardSuggestDto(dtos);
    }
    public ResponseBoardCommentDto getBoardComments(int page, int count, Long boardId){

        if(boardRepository.findByBoardId(boardId).isEmpty())
            throw new CustomException(ErrorCode.NOT_FOUND_BOARD);

        Pageable pageable = PageRequest.of(page-1, count);
        Page<Comment> comments = commentRepository.findAllByBoardBoardId(pageable, boardId);
        List<BoardCommentDto> dtos = new ArrayList<>();
        int total = 0;
        for(Comment comment : comments){
            ResponseBoardUserDto user = UserMapper.INSTANCE.toResponseBoardUserDto(comment.getUser());

            dtos.add(BoardMapper.INSTANCE.toBoardCommentDto(comment,user));
            total++;
        }

        return new ResponseBoardCommentDto(dtos,total);
    }

    public ResponseBoardImageDto postBoardImage(RequestBoardImageDto dto, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));


        UploadFileDto uploadedFile = fileStore.storeFile("dummyImages/", dto.getImage());

        DummyImage dummyImage = new DummyImage();

        dummyImage.setImagePath(uploadedFile.getStorePath());
        dummyImage.setImageType(uploadedFile.getType());
        dummyImage.setImageSize(uploadedFile.getSize());

        DummyImage savedImage = dummyImageRepository.save(dummyImage);

        return new ResponseBoardImageDto(savedImage.getImageId(),savedImage.getImagePath());
    }

    public HttpStatus postBoard(RequestBoardPostDto boardPostDto, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));


        List<Long> dummyImageIds = boardPostDto.getImageIds();
        List<DummyImage> dummyImages = new ArrayList<>();
        for(Long imageId : dummyImageIds){
            DummyImage image = dummyImageRepository.findDummyImageByImageId(imageId);
            if(image != null)
                dummyImages.add(image);
            else
                throw new CustomException(ErrorCode.NOT_FOUND_IMAGE);
        }

        Board board = new Board();
        board.setTitle(boardPostDto.getTitle());
        board.setContent(boardPostDto.getContent());
        board.setBoardType(kindRepository.findBoardTypeByTypeId(Character.forDigit(boardPostDto.getBoardType(),10)));
        Board savedBoard = boardRepository.save(board);

        for(DummyImage image : dummyImages){
            BoardImage boardImage = new BoardImage(savedBoard, image.getImagePath(), image.getImageType(), image.getImageSize());
            boardImageRepository.save(boardImage);
            dummyImageRepository.delete(image);
        }

        List<Tag> tags = new ArrayList<>();
        List<String> tagNames = boardPostDto.getTags();
        for (String tagName : tagNames) {
            Tag tag = new Tag(savedBoard, tagName);
            tags.add(tag);
        }

        tagRepository.saveAll(tags);

        return HttpStatus.OK;
    }

    public HttpStatus postBoardRecommend(Long boardId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        if(recommendRepository.findByBoard_BoardIdAndUser_UserId(boardId, loginUserId) != null)
            throw new CustomException(ErrorCode.DUPLICATE_RECOMMEND);


        BoardRecommend recommend = new BoardRecommend(user, board);

        recommendRepository.save(recommend);
        board.setRecommendCount(recommendRepository.countByBoard_BoardId(boardId));
        boardRepository.save(board);

        return HttpStatus.CREATED;
    }
    public HttpStatus postBoardComment(Long boardId, RequestBoardComment requestBoardComment, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        Comment comment = new Comment(user, board, null, requestBoardComment.getContent());

        commentRepository.save(comment);

        return HttpStatus.CREATED;
    }

    public HttpStatus postBoardAdopt(Long boardId, Long commentId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        Comment comment = commentRepository.findCommentByCommentId(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        if(comment.getBoard() == null)
            throw new CustomException(ErrorCode.NOT_MATCHED_BOARD);

        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        if(board.getAdopt().getCommentId() != null)
            throw new CustomException(ErrorCode.DUPLICATE_ADOPT);

        if(!comment.getBoard().getBoardId().equals(board.getBoardId()))
            throw new CustomException(ErrorCode.NOT_MATCHED_BOARD);

        board.setAdopt(comment);
        boardRepository.save(board);


        return HttpStatus.CREATED;
    }


    public HttpStatus postBoardView(Long boardId, Long loginUserId){
        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        BoardView boardView = boardViewRepository.findByBoard_BoardIdAndUserUserId(boardId, loginUserId);

        if(boardView == null)
        {
            BoardView view = new BoardView();
            view.setBoard(board);
            view.setUser(user);
            boardViewRepository.save(view);

            board.setViewCount((long) boardViewRepository.countByBoard_BoardId(boardId));
            boardRepository.save(board);
        }

        return HttpStatus.CREATED;
    }
    public HttpStatus patchBoard(RequestBoardUpdateDto boardUpdateDto,Long boardId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        if(!Objects.equals(user.getUserId(), board.getUser().getUserId()))
            throw new CustomException(ErrorCode.NOT_MATCHED_USER);

        if(board.getBoardType().getTypeId() == '1' || board.getBoardType().getTypeId() == '2'){
            if(boardUpdateDto.getBoardType() == 3L || boardUpdateDto.getBoardType() == 4L)
                throw new CustomException(ErrorCode.BAD_REQUEST_BOARD_TYPE);
        }
        if(board.getBoardType().getTypeId() == '3' || board.getBoardType().getTypeId() == '4'){
            if(boardUpdateDto.getBoardType() == 1L || boardUpdateDto.getBoardType() == 2L)
                throw new CustomException(ErrorCode.BAD_REQUEST_BOARD_TYPE);
        }


        List<Tag> existingTags = tagRepository.findAllByBoard_BoardId(board.getBoardId());

        List<String> newTags = boardUpdateDto.getTags();

        for (Tag tag : existingTags) {
            if (!newTags.contains(tag.getContent())) {
                tagRepository.delete(tag);
            } else {
                newTags.remove(tag.getContent());
            }
        }

        // 새로운 태그 추가
        if (newTags != null) {
            for (String content : newTags) {
                Tag tag = new Tag(board, content);
                tagRepository.save(tag);
            }
        }

        board.setTitle(boardUpdateDto.getTitle());
        board.setContent(boardUpdateDto.getContent());
        board.setBoardType(kindRepository.findBoardTypeByTypeId(Character.forDigit(boardUpdateDto.getBoardType(),10)));
        board.setUpdatedTime(LocalDateTime.now());

        boardRepository.save(board);

        return HttpStatus.OK;
    }
    public HttpStatus deleteBoardRecommend(Long boardId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));
        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        BoardRecommend recommend = recommendRepository.findByBoard_BoardIdAndUser_UserId(boardId, user.getUserId());

        if(recommend == null)
            throw new CustomException(ErrorCode.NOT_FOUND_RECOMMEND);

        if(!recommend.getUser().getUserId().equals(loginUserId))
            throw new CustomException(ErrorCode.NOT_MATCHED_USER);

        recommendRepository.delete(recommend);
        board.setRecommendCount(recommendRepository.countByBoard_BoardId(boardId));
        boardRepository.save(board);

        return HttpStatus.OK;
    }
    public HttpStatus deleteBoard(Long boardId, Long loginUserId){

        AppUser user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Board board = boardRepository.findByBoardId(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BOARD));

        if(board.getDeleted())
            throw new CustomException(ErrorCode.DELETED_BOARD);

        if(!board.getUser().getUserId().equals(loginUserId))
            throw new CustomException(ErrorCode.NOT_MATCHED_USER);

        board.setDeleted(true);
        board.setDeletedTime(LocalDateTime.now());

        boardRepository.save(board);

        return HttpStatus.OK;
    }
}
