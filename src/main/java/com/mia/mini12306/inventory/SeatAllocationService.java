package com.mia.mini12306.inventory;

import com.mia.mini12306.inventory.mapper.SeatSegmentMapper;
import com.mia.mini12306.order.IncompleteTripStructureException;
import com.mia.mini12306.order.SeatNotAvailableException;
import org.springframework.stereotype.Service;

@Service
public class SeatAllocationService {

    private final SeatSegmentMapper seatSegmentMapper;

    public SeatAllocationService(SeatSegmentMapper seatSegmentMapper) {
        this.seatSegmentMapper = seatSegmentMapper;
    }

    public SeatCandidate allocate(
            Long trainTripId,
            int seatType,
            int departureSequence,
            int arrivalSequence
    ) {
        SeatCandidate candidate = seatSegmentMapper.findFirstAvailableSeat(
                trainTripId, seatType, departureSequence, arrivalSequence
        );

        if(candidate == null) {
            throw describeMissingSeat(
                    trainTripId, seatType, departureSequence, arrivalSequence
            );
        }

        return candidate;
    }

    public void occupy(
            Long trainTripId,
            Long seatId,
            Long orderId,
            int departureSequence,
            int arrivalSequence
    ) {
        int expected = arrivalSequence - departureSequence;

        int occupied = seatSegmentMapper.occupy(
                trainTripId, seatId, orderId,
                departureSequence, arrivalSequence
        );

        // A partial occupation must roll back instead of leaving half a seat.
        if(occupied != expected) {
            throw new IllegalStateException(
                    "Seat occupied did not match the interval"
            );
        }
    }

    public void release(
            Long orderId,
            int departureSequence,
            int arrivalSequence
    ) {
        int expected = arrivalSequence - departureSequence;
        int released = seatSegmentMapper.releaseByOrderId(orderId);

        // The order must give back exactly what it took.
        if(released != expected) {
            throw new IllegalStateException(
                    "Seat release did not match the interval"
            );
        }
    }

    private RuntimeException describeMissingSeat(
            Long trainTripId,
            int seatType,
            int departureSequence,
            int arrivalSequence
    ) {
        if(seatSegmentMapper.countSeatsByType(trainTripId, seatType) == 0) {
            return SeatNotAvailableException.notConfigured();
        }

        // A broken seat layout is not the same as a sold-out interval.
        if(seatSegmentMapper.countSeatsMissingSegments(
                trainTripId, seatType, departureSequence, arrivalSequence
        ) > 0) {
            return new IncompleteTripStructureException();
        }

        return SeatNotAvailableException.soldOut();
    }
}
