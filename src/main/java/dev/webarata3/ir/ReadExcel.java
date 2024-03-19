package dev.webarata3.ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadExcel {
    private static final String BASE_FOLDER = "/Users/arata/dev/e/e2022-06/ir-json";

    private List<Station> stations;

    private static final List<StationTime> SUSPENDED_HOLIDAYS_EAST = List.of(new StationTime(10, "6:57"),
            new StationTime(13, "7:40"), new StationTime(16, "8:12"), new StationTime(13, "8:05"),
            new StationTime(16, "8:38"), new StationTime(16, "17:17"), new StationTime(2, "17:03"),
            new StationTime(16, "18:15"), new StationTime(16, "19:03"), new StationTime(10, "18:53"),
            new StationTime(16, "19:31"), new StationTime(16, "20:20"));

    private static final List<StationTime> SUSPENDED_WEEKDAYS_EAST = List.of(new StationTime(16, "8:38"),
            new StationTime(6, "10:45"));

    private static final List<StationTime> SUSPENDED_HOLIDAYS_TSUBATA_EAST = List.of(new StationTime(16, "16:09"));
    private static final List<StationTime> SUSPENDED_HOLIDAYS_WEST = List.of();

    private static final List<StationTime> SUSPENDED_WEEKDAYS_WEST = List.of();

    private static final List<StationTime> SUSPENDED_HOLIDAYS_TSUBATA_WEST = List.of();

    private ReadExcel() {
        var stationPath = Path.of(BASE_FOLDER, "output/station.json");
        try {
            var stationString = Files.readString(stationPath);
            var listType = new TypeReference<List<Station>>() {
            };
            var mapper = new ObjectMapper();
            stations = mapper.readValue(stationString, listType);
            stations.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var readExcel = new ReadExcel();
        readExcel.readAll();
    }

    public void readAll() {
        var path = Path.of(BASE_FOLDER, "input/20240316.xlsx");
        try (var wb = WorkbookFactory.create(Files.newInputStream(path))) {
            var eastTimetables = readToEast(wb);
            var westTimetables = readToWest(wb);

            var eastPath = Path.of(BASE_FOLDER, "output/east.json");
            var westPath = Path.of(BASE_FOLDER, "output/west.json");
            var mapper = new ObjectMapper();
            mapper.writeValue(Files.newOutputStream(eastPath), eastTimetables);
            mapper.writeValue(Files.newOutputStream(westPath), westTimetables);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Timetable> readToEast(Workbook wb) {
        var sheet = wb.getSheet("Table 1");
        var list1 = readSheet(sheet, 3, 42, 3, 31);
        var list2 = readSheet(sheet, 3, 42, 35, 63);
        var list = Stream.concat(list1.stream(), list2.stream()).toList();

        var timetables = new ArrayList<Timetable>();
        List<StationTime> stationTimes = null;
        for (var inList : list) {
            var index = 0;
            System.out.println(inList.size() + " : " + inList);

            if (stationTimes != null && !stationTimes.isEmpty()) {
                setTimetableEast(timetables, stationTimes);
            }
            stationTimes = new ArrayList<StationTime>();
            for (var i = 0; i < inList.size(); i++) {
                var value = inList.get(i);
                if (i == 18) {
                    index = 5;
                    if (value.equals("直通")) {
                        stationTimes.removeLast();
                    } else {
                        if (!stationTimes.isEmpty()) {
                            setTimetableEast(timetables, stationTimes);
                            stationTimes = new ArrayList<StationTime>();
                        }
                    }
                    continue;
                }
                if (value.isEmpty()) continue;
                if (i == 17 || i == 19 || i == 20) continue;
                // 津幡の発時刻があれば、着時刻を削除
                if (i == 25 && !value.isEmpty()) {
                    stationTimes.removeLast();
                    index++;
                }
                stationTimes.add(new StationTime(i - index, value));
            }
        }
        if (!stationTimes.isEmpty()) {
            setTimetableEast(timetables, stationTimes);
        }

        return timetables;
    }

    public List<Timetable> readToWest(Workbook wb) {
        var sheet = wb.getSheet("Table 2");
        var list1 = readSheet(sheet, 3, 39, 4, 32);
        var list2 = readSheet(sheet, 3, 39, 36, 64);
        var list = Stream.concat(list1.stream(), list2.stream()).toList();

        var timetables = new ArrayList<Timetable>();
        List<StationTime> stationTimes = null;
        for (var inList : list) {
            var index = 0;
            System.out.println(inList.size() + " : " + inList);

            if (stationTimes != null && !stationTimes.isEmpty()) {
                setTimetableEast(timetables, stationTimes);
            }
            stationTimes = new ArrayList<StationTime>();
            for (var i = 0; i < inList.size(); i++) {
                var value = inList.get(i);
                // 津幡は着時刻を飛ばす
                if (i == 3) {
                    index = 1;
                    continue;
                }
                if (i == 12) {
                    index = 6;
                    if (!stationTimes.isEmpty()) {
                        setTimetableEast(timetables, stationTimes);
                        stationTimes = new ArrayList<StationTime>();
                    }
                }
                if (value.isEmpty()) continue;
                stationTimes.add(new StationTime(22 - i + index, value));
            }
        }
        if (!stationTimes.isEmpty()) {
            setTimetableEast(timetables, stationTimes);
        }

        return timetables;
    }

    private void setTimetableEast(List<Timetable> timetables, List<StationTime> stationTimes) {
        var checkTime = stationTimes.get(0);
        var suspendedType = 0;
        if (SUSPENDED_HOLIDAYS_EAST.contains(checkTime)) {
            suspendedType = 1;
        } else if (SUSPENDED_HOLIDAYS_TSUBATA_EAST.contains(checkTime)) {
            suspendedType = 2;
        } else if (SUSPENDED_WEEKDAYS_EAST.contains(checkTime)) {
            suspendedType = 3;
        }
        timetables.add(new Timetable(suspendedType, stationTimes));
    }

    private void setTimetableWest(List<Timetable> timetables, List<StationTime> stationTimes) {
        var checkTime = stationTimes.get(0);
        var suspendedType = 0;
        if (SUSPENDED_HOLIDAYS_WEST.contains(checkTime)) {
            suspendedType = 1;
        } else if (SUSPENDED_HOLIDAYS_TSUBATA_WEST.contains(checkTime)) {
            suspendedType = 2;
        } else if (SUSPENDED_WEEKDAYS_WEST.contains(checkTime)) {
            suspendedType = 3;
        }
        timetables.add(new Timetable(suspendedType, stationTimes));
    }

    private List<List<String>> readSheet(Sheet sheet, int startX, int endX, int startY, int endY) {
        var list = new ArrayList<List<String>>();
        for (var x = startX; x <= endX; x++) {
            var inList = new ArrayList<String>();
            list.add(inList);
            for (var y = startY; y <= endY; y++) {
                var row = sheet.getRow(y);
                var cell = row.getCell(x);
                System.out.println(x + "," + y);
                var value = cell.getStringCellValue().trim();
                if (value.matches("[0-9]?[0-9]:[0-9][0-9]") || value.equals("直通")) {
                    inList.add(value);
                } else {
                    inList.add("");
                }
            }
        }
        return list;
    }
}
