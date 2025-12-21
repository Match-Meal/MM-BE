package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.FoodAnalysisResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AiFoodVisionService {

    FoodAnalysisResponseDto analyzeAndFindFood(MultipartFile image);
}
