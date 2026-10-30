package com.mia.mini12306.train;

import com.mia.mini12306.station.mapper.StationQueryMapper;
import com.mia.mini12306.train.dto.TicketListResponse;
import com.mia.mini12306.train.dto.TicketQueryRequest;
import com.mia.mini12306.train.dto.TrainTripSearchRequest;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainTripService {

    private final TrainTripMapper trainTripMapper;
    private final StationQueryMapper stationQueryMapper;

    public TrainTripService(
            TrainTripMapper trainTripMapper,
            StationQueryMapper stationQueryMapper
    ) {
        this.trainTripMapper = trainTripMapper;
        this.stationQueryMapper = stationQueryMapper;
    }

    public List<TrainTrip> search(TrainTripSearchRequest request) {

        if(request.fromStation().equals(request.toStation())) {
            throw new InvalidTrainTripSearchException();
        }

        // Build the departure-time range for the requested date.
        LocalDateTime dayStart = request.departureDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);

        return trainTripMapper.findByRouteAndDepartureDay(
                request.fromStation(),
                request.toStation(),
                dayStart,
                nextDayStart
        );
    }

    public List<TicketListResponse> searchInterval(TicketQueryRequest request) {

        // Same rule as the reference project: identical endpoints are rejected first.
        if(request.fromStation().equals(request.toStation())) {
            throw new InvalidTrainTripSearchException();
        }

        // An unknown code cannot describe a route, so reject it before the join.
        if(stationQueryMapper.countByCodes(
                request.fromStation(), request.toStation()) < 2
        ) {
            throw new StationCodeNotFoundException();
        }

        LocalDateTime dayStart = request.departureDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);

        // The date belongs to the boarding stop, not to the trip origin.
        return trainTripMapper.findIntervalsByCodesAndDepartureDay(
                request.fromStation(),
                request.toStation(),
                dayStart,
                nextDayStart
        ).stream().map(TicketListResponse::from).toList();
    }
}
