package com.mia.mini12306.train;

import com.mia.mini12306.train.dto.TrainTripSearchRequest;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainTripService {

    private final TrainTripMapper trainTripMapper;

    public TrainTripService(TrainTripMapper trainTripMapper) {
        this.trainTripMapper = trainTripMapper;
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
}
