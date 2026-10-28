package com.mia.mini12306.passenger.controller;

import com.mia.mini12306.passenger.Passenger;
import com.mia.mini12306.passenger.PassengerService;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.user.CurrentUserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerService passengerService;
    private final CurrentUserSession currentUserSession;

    public PassengerController(
            PassengerService passengerService,
            CurrentUserSession currentUserSession
    ) {
        this.passengerService = passengerService;
        this.currentUserSession = currentUserSession;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody PassengerSaveRequest request,
            HttpServletRequest httpRequest
    ) {
        // Derive ownership from the server-side session.
        Long userId = currentUserSession.requireUser(httpRequest).id();
        passengerService.create(userId, request);
    }

    @GetMapping
    public List<PassengerResponse> findAll(HttpServletRequest httpRequest) {
        Long userId = currentUserSession.requireUser(httpRequest).id();

        // Map entities to the public response format.
        return passengerService.findAll(userId).stream()
                .map(PassengerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PassengerResponse findById(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserSession.requireUser(httpRequest).id();
        return PassengerResponse.from(
                passengerService.findById(userId, id)
        );
    }

    @PutMapping("/{id}")
    public PassengerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PassengerSaveRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserSession.requireUser(httpRequest).id();

        // The service applies both the record ID and owner ID.
        return PassengerResponse.from(
                passengerService.update(userId, id, request)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserSession.requireUser(httpRequest).id();
        passengerService.delete(userId, id);
    }
}
