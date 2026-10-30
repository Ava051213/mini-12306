package com.mia.mini12306.station.controller;

import com.mia.mini12306.station.StationQueryService;
import com.mia.mini12306.station.dto.StationResponse;
import com.mia.mini12306.station.dto.TrainStopResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-service")
public class StationController {

    private final StationQueryService stationQueryService;

    public StationController(StationQueryService stationQueryService) {
        this.stationQueryService = stationQueryService;
    }

    @GetMapping("/station/all")
    public List<StationResponse> listStations() {
        return stationQueryService.listStations();
    }

    @GetMapping("/train-station/query")
    public List<TrainStopResponse> listTrainStops(
            @RequestParam @Positive Long trainId
    ) {
        return stationQueryService.listTrainStops(trainId);
    }
}
