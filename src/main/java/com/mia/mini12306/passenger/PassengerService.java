package com.mia.mini12306.passenger;

import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.passenger.mapper.PassengerMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PassengerService {

    private final PassengerMapper passengerMapper;

    public PassengerService(PassengerMapper passengerMapper) {
        this.passengerMapper = passengerMapper;
    }

    public void create(Long userId, PassengerSaveRequest request) {
        Passenger passenger = fromRequest(userId, request);
        try {
            passengerMapper.insert(passenger);
        } catch(DuplicateKeyException exception) {
            throw new PassengerAlreadyExistsException();
        }
    }

    public List<Passenger> findAll(Long userId) {
        return passengerMapper.findAllByUserId(userId);
    }

    public Passenger findById(Long userId, long passengerId) {
        Passenger passenger =
                passengerMapper.findByIdAndUserId(passengerId, userId);
        if(passenger == null) {
            throw new PassengerNotFoundException();
        }
        return passenger;
    }

    public Passenger update(
            Long userId,
            Long passengerId,
            PassengerSaveRequest request
    ) {
        Passenger passenger = fromRequest(userId, request);
        passenger.setId(passengerId);

        try {
            passengerMapper.update(passenger);
        } catch(DuplicateKeyException exception) {
            throw new PassengerAlreadyExistsException();
        }

        return findById(userId, passengerId);
    }

    private Passenger fromRequest(
            Long userId,
            PassengerSaveRequest request
    ) {
        Passenger passenger = new Passenger();
        passenger.setUserId(userId);
        passenger.setRealName(request.realName());
        passenger.setIdCard(request.idCard().toUpperCase(Locale.ROOT));
        passenger.setPhone(request.phone());
        return passenger;
    }
}
