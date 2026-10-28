package com.mia.mini12306.passenger;

import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.passenger.mapper.PassengerMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void delete(Long userId, Long passengerId) {
        // Check ownership first so foreign records look exactly like missing ones.
        findById(userId, passengerId);

        // Orders keep their own snapshot, but still reference this row.
        if(passengerMapper.countOrdersByPassengerId(passengerId) > 0) {
            throw new PassengerInUseException();
        }

        try {
            // The owner condition makes foreign rows impossible to delete.
            if(passengerMapper.deleteByIdAndUserId(passengerId, userId) != 1) {
                throw new PassengerNotFoundException();
            }
        } catch(DataIntegrityViolationException exception) {
            // A concurrent order may appear after the check; the foreign key blocks it.
            throw new PassengerInUseException();
        }
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
