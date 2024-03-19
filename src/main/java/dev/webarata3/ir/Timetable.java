package dev.webarata3.ir;

import java.util.List;

public record Timetable(int suspendedType, List<StationTime> stationTimes) {
}
