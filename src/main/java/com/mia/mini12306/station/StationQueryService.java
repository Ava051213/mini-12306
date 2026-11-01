package com.mia.mini12306.station;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mia.mini12306.station.dto.StationResponse;
import com.mia.mini12306.station.dto.TrainStopResponse;
import com.mia.mini12306.station.mapper.StationQueryMapper;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationQueryService {

    private final StationQueryMapper stationQueryMapper;
    private final TrainTripMapper trainTripMapper;

    public StationQueryService(
            StationQueryMapper stationQueryMapper,
            TrainTripMapper trainTripMapper
    ) {
        this.stationQueryMapper = stationQueryMapper;
        this.trainTripMapper = trainTripMapper;
    }

    public List<StationResponse> listStations() {
        LambdaQueryWrapper<Station> query = new LambdaQueryWrapper<>();
        query.orderByAsc(Station::getCode);

        return stationQueryMapper.selectList(query).stream()
                .map(StationResponse::from)
                .toList();
    }

    public List<TrainStopResponse> listTrainStops(Long trainTripId) {
        // Missing trip is 404; an existing trip without stops is an empty list.
        if(trainTripMapper.countById(trainTripId) == 0) {
            throw new TrainTripNotFoundException();
        }
        return stationQueryMapper.findStopsByTrainTripId(trainTripId).stream()
                .map(TrainStopResponse::from)
                .toList();
    }
}
