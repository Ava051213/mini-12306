package com.mia.mini12306.train.controller;

import com.mia.mini12306.train.TrainTripService;
import com.mia.mini12306.train.dto.TrainTripResponse;
import com.mia.mini12306.train.dto.TrainTripSearchRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainTripController {

    private final TrainTripService trainTripService;

    public TrainTripController(TrainTripService trainTripService) {
        this.trainTripService = trainTripService;
    }

    @GetMapping
    public List<TrainTripResponse> search(
            @Valid @ModelAttribute TrainTripSearchRequest request
    ) {
        // Return only the public response fields.
        return trainTripService.search(request).stream()
                .map(TrainTripResponse::from)
                .toList();
    }
}
