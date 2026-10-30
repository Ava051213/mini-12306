package com.mia.mini12306.station.dto;

import com.mia.mini12306.station.Station;

public record StationResponse(
        String name,
        String code,
        String spell,
        String regionName
) {

    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getName(),
                station.getCode(),
                station.getSpell(),
                station.getRegionName()
        );
    }
}
