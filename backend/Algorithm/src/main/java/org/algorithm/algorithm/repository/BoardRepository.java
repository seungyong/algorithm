package org.algorithm.algorithm.repository;

import org.algorithm.algorithm.entity.BoardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    BoardEntity findBoardEntityByBoardId(long boardId);
    Page<BoardEntity> findBoardEntitiesByBoardType(Pageable pageable, long boardType);
    Page<BoardEntity> findBoardEntitiesByBoardTypeIn(Pageable pageable, List<Long> boardTypes);

    Page<BoardEntity> findBoardEntitiesByBoardTypeAndAlgorithmId(Pageable pageable, Long boardType, Long algorithmId);
    Page<BoardEntity> findBoardEntitiesByBoardTypeInAndAlgorithmId(Pageable pageable, List<Long> boardTypes, Long algorithmId);

    @Query(value = "SELECT b.* " +
            "FROM board b " +
            "INNER JOIN ( " +
            "    SELECT board_id, COUNT(board_id) AS view_count " +
            "    FROM board_view " +
            "    WHERE created_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "    GROUP BY board_id " +
            "    ORDER BY view_count DESC " +
            "    LIMIT 6 " +
            ") bv ON b.board_id = bv.board_id;",
            nativeQuery = true)
    List<BoardEntity> findRecommendedBoardEntities();
    @Query(value = "SELECT COUNT(*) FROM comment where board_id = :boardId",
            nativeQuery = true)
    String findCommentCountByBoardId(long boardId);


}
