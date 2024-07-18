package com.college.algorithm.repository;

import com.college.algorithm.entity.Board;
import com.college.algorithm.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardImageRepository extends JpaRepository<BoardImage,Long> {
    BoardImage findByBoardOrderByCreatedTimeAsc(Board board);
}
