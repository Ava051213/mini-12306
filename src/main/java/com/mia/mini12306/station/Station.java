package com.mia.mini12306.station;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Station {

    private Long id;
    private String code;
    private String name;
    private String spell;
    private String regionName;
}
