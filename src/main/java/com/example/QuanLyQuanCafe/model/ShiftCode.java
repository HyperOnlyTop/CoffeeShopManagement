package com.example.QuanLyQuanCafe.model;

import java.time.LocalTime;

public enum ShiftCode {
    MORNING(LocalTime.of(7, 0), LocalTime.of(14, 0)),
    AFTERNOON(LocalTime.of(14, 0), LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(22, 0)),
    FULL_DAY(LocalTime.of(7, 0), LocalTime.of(22, 0));

    private final LocalTime start;
    private final LocalTime end;

    ShiftCode(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}

