package com.college.algorithm.service;

import com.college.algorithm.dto.*;
import com.college.algorithm.entity.*;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.mapper.BoardMapper;
import com.college.algorithm.mapper.CommentMapper;
import com.college.algorithm.mapper.UserMapper;
import com.college.algorithm.repository.*;
import com.college.algorithm.util.FileStore;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    @Value("${default.profile}")
    private String defaultProfilePath;
    @Value("${spring.mail.username}")
    private String sender;

    private final UserRepository userRepository;
    private final AlgorithmRecommendRepository algorithmRecommendRepository;
    private final EmailVerifyRepository emailVerifyRepository;
    private final UserLinkRepository userLinkRepository;
    private final BoardTypeRepository boardTypeRepository;
    private final BoardRepository boardRepository;
    private final BoardRecommendRepository boardRecommendRepository;
    private final CommentRepository commentRepository;

    private final FileStore fileStore;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;

    public ResponseUserDto getMyInfo(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return UserMapper.INSTANCE.toResponseUserDto(user);
    }

    public ResponseOtherUserDto getUser(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        int favorite = algorithmRecommendRepository.countByUser(user);

        return UserMapper.INSTANCE.toResponseOtherUserDto(user, favorite);
    }

    public ResponseUserLinkDto getLinks(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        List<UserLink> links = userLinkRepository.findAllByUser(user);
        List<ResponseUserLinkDto.LinkDto> linkDtos = links.stream()
                .map(UserMapper.INSTANCE::toResponseLinkDto)
                .toList();

        return new ResponseUserLinkDto(linkDtos);
    }

    public ResponseMyAlgorithmDto getMyAlgorithms(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        int favorite = algorithmRecommendRepository.countByUser(user);

        return UserMapper.INSTANCE.toResponseMyAlgorithmDto(user, favorite);
    }

    public ResponseNotificationSettingsDto getNotificationSettings(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return UserMapper.INSTANCE.toResponseNotificationSettingsDto(user);
    }

    public ResponseMyBoardDto getMyHistories(long userId, int page, int count, List<String> types) {
        Pageable pageable = PageRequest.of(page - 1, count);

        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        List<BoardType> boardTypes = boardTypeRepository.findAllByTypeNameIn(types);
        if (boardTypes == null || boardTypes.isEmpty()) { throw new CustomException(ErrorCode.NOT_FOUND_BOARD_TYPE); }

        Page<Board> boardPage = boardRepository.findAllByBoardTypeInAndUserAndDeletedIsFalseOrderByCreatedTimeDesc(
                boardTypes,
                user,
                pageable
        );

        List<Board> boards = boardPage.getContent();
        List<BoardIntroDto> introDtos = null;
        boolean isFreeBoard = boardTypes.stream().allMatch((type) -> type.getTypeName().equals(com.college.algorithm.type.BoardType.GENERAL_FREE.getTypeName()));

        if (isFreeBoard) {
            introDtos = boards.stream()
                    .map(BoardMapper.INSTANCE::toResponseBoardIntroWithoutSolvedDto)
                    .toList();
        } else {
            introDtos = boards.stream()
                    .map(BoardMapper.INSTANCE::toResponseBoardIntroDto)
                    .toList();
        }

        long total = boardPage.getTotalElements();

        return new ResponseMyBoardDto(introDtos, total);
    }

    public ResponseMyCommentDto getMyAdopts(long userId, int page, int count) {
        Pageable pageable = PageRequest.of(page - 1, count);

        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Page<Board> boardPage = boardRepository.findAllByUserAndAdoptIdIsNotNullAndDeletedIsFalseOrderByCreatedTimeDesc(
                user,
                pageable
        );
        List<Board> boards = boardPage.getContent();

        List<Long> commentIds = boards.stream()
                .map(Board::getAdoptId)
                .filter(Objects::nonNull) // null 값 제외
                .toList();
        List<Comment> comments = commentRepository.findAllByCommentIdIn(commentIds);

        // adoptId와 Comment 객체를 매핑하는 Map 생성
        Map<Long, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getCommentId, comment -> comment));

        // ResponseMyCommentDto.CommentDto 리스트 생성
        List<ResponseMyCommentDto.CommentDto> commentDtos = boards.stream()
                .filter(board -> board.getAdoptId() != null)
                .map(board -> CommentMapper.INSTANCE.toResponseCommentWithoutSolvedDto(board, commentMap.get(board.getAdoptId())))
                .collect(Collectors.toList());

        long total = boardPage.getTotalElements();

        return new ResponseMyCommentDto(commentDtos, total);
    }

    public ResponseMyCommentDto getMyComments(long userId, int page, int count) {
        Pageable pageable = PageRequest.of(page - 1, count);

        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        BoardType freeType = boardTypeRepository.findAllByTypeName(com.college.algorithm.type.BoardType.GENERAL_FREE.getTypeName());

        Page<Comment> commentPage = commentRepository.findAllByUserAndBoardDeletedIsFalseOrderByCreatedTimeDesc(
                user,
                pageable
        );
        List<Comment> comments = commentPage.getContent();

        List<ResponseMyCommentDto.CommentDto> commentDtos = comments.stream()
                .map((comment) -> {
                    long boardType = comment.getBoard().getBoardType().getTypeId();

                    if (boardType == freeType.getTypeId()) {
                        return  CommentMapper.INSTANCE.toResponseCommentWithoutSolvedDto(comment.getBoard(), comment);
                    } else {
                        Board board = comment.getBoard();
                        boolean isSolved = board.getAdoptId() != null && board.getAdoptId().equals(comment.getCommentId());
                        return CommentMapper.INSTANCE.toResponseCommentDto(board, comment, isSolved);
                    }
                })
                .toList();
        long total = commentPage.getTotalElements();

        return new ResponseMyCommentDto(commentDtos, total);
    }

    public ResponseMyBoardDto getMyRecommends(long userId, int page, int count) {
        Pageable pageable = PageRequest.of(page - 1, count);

        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        BoardType freeType = boardTypeRepository.findAllByTypeName(com.college.algorithm.type.BoardType.GENERAL_FREE.getTypeName());

        Page<BoardRecommend> recommendPage = boardRecommendRepository.findAllByUserAndBoardDeletedIsFalseOrderByCreatedTimeDesc(
                user,
                pageable
        );
        List<BoardRecommend> recommends = recommendPage.getContent();
        List<BoardIntroDto> boardDtos = recommends.stream()
                .map((recommend) -> {
                    long boardType = recommend.getBoard().getBoardType().getTypeId();

                    if (boardType == freeType.getTypeId()) {
                        return  BoardMapper.INSTANCE.toResponseBoardIntroWithoutSolvedDto(recommend.getBoard());
                    } else {
                        return BoardMapper.INSTANCE.toResponseBoardIntroDto(recommend.getBoard());
                    }
                })
                .toList();
        long total = recommendPage.getTotalElements();

        return new ResponseMyBoardDto(boardDtos, total);
    }

    public Long login(RequestLoginDto dto) {
        AppUser user = userRepository.findByEmail(dto.getEmail())
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if (!passwordEncoder.matches(dto.getUserPw(), user.getUserPw())) { throw new CustomException(ErrorCode.NOT_MATCH_PASSWORD); }
        return user.getUserId();
    }

    public void signup(RequestSignupDto dto) {
        boolean hasNicknameUser = userRepository.existsByNickname(dto.getNickname());
        boolean hasEmailUser = userRepository.existsByEmail(dto.getEmail());
        if (hasNicknameUser) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_NICKNAME); }
        if (hasEmailUser) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_EMAIL); }

        EmailVerify emailVerify = emailVerifyRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_VERIFIED_EMAIL));
        if (!emailVerify.getVerified()) { throw new CustomException(ErrorCode.NOT_VERIFIED_EMAIL); }

        String encryptedPassword = passwordEncoder.encode(dto.getUserPw());

        AppUser user = AppUser.builder()
                .email(dto.getEmail())
                .userPw(encryptedPassword)
                .nickname(dto.getNickname())
                .defaultProfilePath(defaultProfilePath)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void sendEmail(RequestSendEmailDto dto) {
        boolean existsEmail = userRepository.existsByEmail(dto.getEmail());
        if (existsEmail) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_EMAIL); }

        Optional<EmailVerify> emailVerify = emailVerifyRepository.findByEmail(dto.getEmail());
        String verifyCode = RandomStringUtils.random(6, 33, 125, true, true);
        EmailVerify entity;
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        if (emailVerify.isPresent()) {
            entity = emailVerify.get();
            if (entity.getVerified()) {
                entity.setVerified(false);
            }

            entity.setVerifyCode(verifyCode);
            entity.setExpirationTime(expiryTime);
        } else {
            entity = EmailVerify.builder()
                    .email(dto.getEmail())
                    .verifyCode(verifyCode)
                    .expirationTime(expiryTime)
                    .build();
        }

        MimeMessage message = makeEmailBody(verifyCode, dto.getEmail());
        javaMailSender.send(message);

        emailVerifyRepository.save(entity);
    }

    public void compareVerifyCode(RequestCompareVerifycodeDto dto) {
        boolean existsEmail = userRepository.existsByEmail(dto.getEmail());
        if (existsEmail) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_EMAIL); }

        EmailVerify emailVerify = emailVerifyRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_EMAIL_VERIFIED));

        LocalDateTime currentTime = LocalDateTime.now();
        long minutesDifference = ChronoUnit.MINUTES.between(currentTime, emailVerify.getExpirationTime());
        if (minutesDifference < 0) { throw new CustomException(ErrorCode.EXPIRED_TIME_EMAIL); }

        if (!emailVerify.getVerifyCode().equals(dto.getVerifyCode())) { throw new CustomException(ErrorCode.NOT_MATCH_VERIFY_CODE); }

        emailVerify.setVerified(true);
        emailVerifyRepository.save(emailVerify);
    }

    @Transactional
    public void addLink(long userId, RequestLinkDto dto) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        UserLink link = userLinkRepository.findByUserAndLinkKind(user, dto.getLinkKind().getKindId());

        if (link == null) {
            link = UserLink.builder()
                    .user(user)
                    .linkKind(dto.getLinkKind().getKindId())
                    .domain(dto.getDomain())
                    .build();
        } else {
            link.setDomain(dto.getDomain());
        }

        userLinkRepository.save(link);
    }

    @Transactional
    public void updateProfile(long userId, RequestUpdateProfileDto dto) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            if (!user.getNickname().equals(dto.getNickname())) {
                boolean hasNicknameUser = userRepository.existsByNickname(dto.getNickname());
                if (hasNicknameUser) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_NICKNAME); }
            }

            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            boolean hasEmailUser = userRepository.existsByEmail(dto.getEmail());
            if (hasEmailUser) { throw new CustomException(ErrorCode.DUPLICATE_PARAMETER_EMAIL); }

            EmailVerify emailVerify = emailVerifyRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_VERIFIED_EMAIL));
            if (!emailVerify.getVerified()) { throw new CustomException(ErrorCode.NOT_VERIFIED_EMAIL); }

            user.setEmail(dto.getEmail());
        }

        userRepository.save(user);
    }

    public void updateProfile(long userId, RequestProfileDto dto) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        String prevProfilePath = user.getProfilePath();

        UploadFileDto uploadedFile = fileStore.storeFile("profile/", dto.getProfile());

        user.setProfilePath(uploadedFile.getStorePath());
        user.setProfileType(uploadedFile.getType());
        user.setProfileSize(uploadedFile.getSize());

        userRepository.save(user);

        if (!prevProfilePath.equals(defaultProfilePath)) {
            fileStore.deleteFile(prevProfilePath);
        }
    }

    public void updateNotification(long userId, RequestNotificationDto dto) {
        AppUser user = userRepository.findByUserId(userId)
                .map(item -> {
                    if (item.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }
                    return item;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        user.setPrimaryNofi(dto.getPrimaryNofi() != null ? dto.getPrimaryNofi() : user.getPrimaryNofi());
        user.setCommentNofi(dto.getCommentNofi() != null ? dto.getCommentNofi() : user.getCommentNofi());
        user.setLikeNofi(dto.getLikeNofi() != null ? dto.getLikeNofi() : user.getLikeNofi());
        user.setAnswerNofi(dto.getAnswerNofi() != null ? dto.getAnswerNofi() : user.getAnswerNofi());
        user.setEventNofi(dto.getEventNofi() != null ? dto.getEventNofi() : user.getEventNofi());

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(long userId) {
        AppUser user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        Optional<EmailVerify> emailVerify = emailVerifyRepository.findByEmail(user.getEmail());

        if (user.getDeleted()) { throw new CustomException(ErrorCode.DELETED_USER); }

        user.setDeleted(true);
        user.setDeletedTime(LocalDateTime.now());

        if (emailVerify.isPresent()) {
            EmailVerify entity = emailVerify.get();
            emailVerifyRepository.delete(entity);
        }

        userRepository.save(user);
    }

    private MimeMessage makeEmailBody(String verifyCode, String toEmail) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(sender);
            message.setRecipients(MimeMessage.RecipientType.TO, toEmail);
            message.setSubject("Algorithm 이메일 인증");

            String body = "<h3>요청하신 인증 번호입니다.</h3>" +
                    "<h1>" + verifyCode + "</h1>" +
                    "<p>1회성 인증 번호이며, 절대 다른 사람에게 양도해서는 안 됩니다.</p>" +
                    "<p>감사합니다.</p>";

            message.setText(body, "UTF-8", "html");
            return message;
        } catch (MessagingException e) {
            log.error("'"+ toEmail + "' 인증 이메일을 보내지 못했습니다.\n이유 : " + e.getMessage());
            throw new CustomException(ErrorCode.ERROR_EMAIL_SENDER);
        }
    }
}
