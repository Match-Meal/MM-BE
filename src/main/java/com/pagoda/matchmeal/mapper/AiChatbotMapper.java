package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.AiChatbot;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface AiChatbotMapper {
    // 1. 대화 내역 저장
    void insertChatLog(AiChatbot aiChatbot);

    // 2. 히스토리 조회 (최신순)
    List<AiChatbot> selectHistoryByUserId(@Param("userId") Long userId);
}
