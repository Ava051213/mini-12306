package com.mia.mini12306.train.controller;

import com.mia.mini12306.train.TrainTripService;
import com.mia.mini12306.train.dto.TicketListResponse;
import com.mia.mini12306.train.dto.TicketPageQueryResponse;
import com.mia.mini12306.train.dto.TicketQueryRequest;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticket-service/ticket")
public class TicketQueryController {

    private final TrainTripService trainTripService;

    public TicketQueryController(TrainTripService trainTripService) {
        this.trainTripService = trainTripService;
    }

    @GetMapping("/query")
    public TicketPageQueryResponse pageListTicketQuery(
            @Valid @ModelAttribute TicketQueryRequest request
    ) {
        // Same path as the reference project; the body keeps the local shape.
        return new TicketPageQueryResponse(
                trainTripService.searchInterval(request)
        );
    }
}
