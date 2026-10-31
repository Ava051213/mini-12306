package com.mia.mini12306.train;

import com.mia.mini12306.station.mapper.StationQueryMapper;
import com.mia.mini12306.train.dto.SeatClassResponse;
import com.mia.mini12306.train.dto.TicketListResponse;
import com.mia.mini12306.train.dto.TicketQueryRequest;
import com.mia.mini12306.train.dto.TrainTripSearchRequest;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<TrainTripInterval> intervals =
                trainTripMapper.findIntervalsByCodesAndDepartureDay(
                        request.fromStation(),
                        request.toStation(),
                        dayStart,
                        nextDayStart
                );

        // An empty list would build "IN()", which MySQL rejects.
        if(intervals.isEmpty()) {
            return List.of();
        }

        // Group once so assembling stays O(n) and keeps the query order.
        Map<IntervalKey, List<SeatClassResponse>> pricesByInterval =
                trainTripMapper.findPricesByIntervals(intervals).stream()
                        .collect(Collectors.groupingBy(
                                IntervalKey::of,
                                Collectors.mapping(
                                        SeatClassResponse::from,
                                        Collectors.toList()
                                )
                        ));

        return intervals.stream()
                .map(interval -> TicketListResponse.from(
                        interval,
                        pricesByInterval.getOrDefault(
                                IntervalKey.of(interval), List.of()
                        )
                )).toList();
    }

    // Same three columns as the unique key, so lookups never mix trips up.
    private record IntervalKey(
            Long trainTripId,
            Integer departureSequence,
            Integer arrivalSequence
    ) {
        static IntervalKey of(TrainTripInterval interval) {
            return new IntervalKey(
                    interval.getTrainTripId(),
                    interval.getDepartureSequence(),
                    interval.getArrivalSequence()
            );
        }

        static IntervalKey of(TrainTripIntervalPrice price) {
            return new IntervalKey(
                    price.getTrainTripId(),
                    price.getDepartureSequence(),
                    price.getArrivalSequence()
            );
        }
    }
}
