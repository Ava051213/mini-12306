package com.mia.mini12306.order;

import com.mia.mini12306.station.RouteBoundaryStop;
import com.mia.mini12306.station.TrainTripNotFoundException;
import com.mia.mini12306.station.TripStopDetail;
import com.mia.mini12306.station.mapper.StationQueryMapper;
import com.mia.mini12306.train.TrainTrip;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class OrderPricingService {

    private final TrainTripMapper trainTripMapper;
    private final StationQueryMapper stationQueryMapper;

    public OrderPricingService(
            TrainTripMapper trainTripMapper,
            StationQueryMapper stationQueryMapper
    ) {
        this.trainTripMapper = trainTripMapper;
        this.stationQueryMapper = stationQueryMapper;
    }

    public OrderPriceSnapshot price(Long trainTripId, int seatType) {
        TrainTrip trip = trainTripMapper.findById(trainTripId);

        if(trip == null) {
            throw new TrainTripNotFoundException();
        }

        List<RouteBoundaryStop> stops =
                stationQueryMapper.findRouteBoundaryStops(trainTripId);

        // A sellable route needs two different ends.
        if(stops.size() != 2
                || stops.get(0).stopSequence() >= stops.get(1).stopSequence()) {
            throw new FullRouteStopsIncompleteException();
        }

        RouteBoundaryStop first = stops.get(0);
        RouteBoundaryStop last = stops.get(1);

        // Stops must describe the same route the trip advertises.
        if(!first.stationName().equals(trip.getDepartureStation())
                || !last.stationName().equals(trip.getArrivalStation())
                || !Objects.equals(first.departureAt(), trip.getDepartureAt())
                || !Objects.equals(last.arrivalAt(), trip.getArrivalAt())) {
            throw new FullRouteStopsIncompleteException();
        }

        Integer amountCents = trainTripMapper.findSeatPrice(
                trainTripId,
                first.stopSequence(),
                last.stopSequence(),
                seatType
        );

        // No fallback to a partial interval or another seat type.
        if(amountCents == null) {
            throw new FullRoutePriceNotConfiguredException();
        }

        return new OrderPriceSnapshot(
                first.stopSequence(),
                last.stopSequence(),
                first.stationCode(),
                first.stationName(),
                first.departureAt(),
                last.stationCode(),
                last.stationName(),
                last.arrivalAt(),
                seatType,
                amountCents
        );
    }

    public OrderPriceSnapshot priceInterval(
            Long trainTripId,
            String fromStationCode,
            String toStationCode,
            int seatType
    ) {
        TrainTrip trip = trainTripMapper.findById(trainTripId);

        if(trip == null) {
            throw new TrainTripNotFoundException();
        }

        List<TripStopDetail> stops = stationQueryMapper.findStopWithCodes(trainTripId);

        // Seat sales assume one segment per adjacent pair of stops.
        validateContinuousSequences(stops);

        TripStopDetail boarding = findUniqueStop(stops, fromStationCode);
        TripStopDetail alighting = findUniqueStop(stops, toStationCode);

        if(boarding.stopSequence() >= alighting.stopSequence()) {
            throw new InvalidTicketIntervalException();
        }

        if(boarding.departureAt() == null
                || alighting.arrivalAt() == null
                || !alighting.arrivalAt().isAfter(boarding.departureAt())) {
            throw new IncompleteTripStructureException();
        }

        Integer amountCents = trainTripMapper.findSeatPrice(
                trainTripId,
                boarding.stopSequence(),
                alighting.stopSequence(),
                seatType
        );

        // No fallback to a partial interval or another seat type.
        if(amountCents == null) {
            throw new IntervalPriceNotConfiguredException();
        }

        return new OrderPriceSnapshot(
                boarding.stopSequence(),
                alighting.stopSequence(),
                boarding.stationCode(),
                boarding.stationName(),
                boarding.departureAt(),
                alighting.stationCode(),
                alighting.stationName(),
                alighting.arrivalAt(),
                seatType,
                amountCents
        );
    }

    private void validateContinuousSequences(List<TripStopDetail> stops) {
        if(stops.size() < 2) {
            throw new IncompleteTripStructureException();
        }

        int first = stops.get(0).stopSequence();
        int last = stops.get(stops.size() - 1).stopSequence();

        // A gap would make the segment span meaningless.
        if(last - first + 1 != stops.size()) {
            throw new IncompleteTripStructureException();
        }
    }

    private TripStopDetail findUniqueStop(
            List<TripStopDetail> stops,
            String stationCode
    ) {
        List<TripStopDetail> matches = stops.stream()
                .filter(stop -> stop.stationCode().equals(stationCode))
                .toList();

        if(matches.isEmpty()) {
            throw new InvalidTicketIntervalException();
        }

        if(matches.size() > 1) {
            throw new AmbiguousTicketIntervalException();
        }

        return matches.get(0);
    }
}
