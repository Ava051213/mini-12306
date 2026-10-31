package com.mia.mini12306.order;

import com.mia.mini12306.station.RouteBoundaryStop;
import com.mia.mini12306.station.TrainTripNotFoundException;
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
}
