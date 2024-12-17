package org.example;

import org.json.simple.JSONArray;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {

        String[] splitCurrentLine;
        ArrayList<Map<String, String>> TSTAT = new ArrayList<>();
        ArrayList<Map<String, String>> BATT = new ArrayList<>();
        ArrayList<Map<String, String>> alertList = new ArrayList<>();

        InputStream inputStream = new InputStream();
        String currentLine = inputStream.nextLine();

        while (!currentLine.isEmpty()){
            // Separate string by delimiter.
            splitCurrentLine = currentLine.split("\\|");
            //Add String Status to List and sort them based on component.
            sortComponent(parseStatus(splitCurrentLine), TSTAT, BATT);
            //Check if queue is within 5 minutes and Output Alert.
            currentLine = inputStream.nextLine();
        }

        //Clean list of entries over 5 minutes
        checkTime(TSTAT);
        checkTime(BATT);

        //Filter only Red alerts.
        TSTAT = alert(TSTAT);
        BATT = alert(BATT);

        //Merge alerts to an alert list.
        TSTAT.forEach(alertList::add);
        BATT.forEach(alertList::add);

        //build JSON array
        JSONArray output = new JSONArray();
        output.addAll(alertList);

        //Print out the Json data.
        System.out.println(output);
    }


    public static Map<String, String> parseStatus(String... input){
        boolean BATT = false;
        boolean TSTAT = false;
        String alertLevel = "";
        Map<String, String> status= new HashMap<>();

        double measurement = Double.parseDouble(input[input.length - 2]);
        double redLowLimit = Double.parseDouble(input[input.length - 3]);
        double yellowLowLimit = Double.parseDouble(input[input.length - 4]);
        double yellowHighLimit = Double.parseDouble(input[input.length - 5]);
        double redHighLimit = Double.parseDouble(input[input.length - 6]);

        //Check if the entry is a BATT or a TSTAT.
        // There are 4 alert thresholds and 4 alarm warnings. See below for additional details.
        if(input[input.length-1].equals("BATT")) BATT = true;
        if(input[input.length-1].equals("TSTAT")) TSTAT = true;
        //TSTAT alerts when the level is above the threshold.
        if(TSTAT){
            if(measurement > redHighLimit ) alertLevel = "red-high-limit";
            else if(measurement > yellowHighLimit) alertLevel = "yellow-high-limit";
            else if(measurement > yellowLowLimit) alertLevel = "yellow-low-Limit";
            else if(measurement > redLowLimit) alertLevel = "red-low-limit";
            else System.out.println("Lower then red-low-limit");
        }
        // BATT alerts when the level is below the threshold.
        if(BATT){
            if(measurement <= redLowLimit ) alertLevel = "red-low-limit";
            else if(measurement <= yellowLowLimit ) alertLevel = "yellow-low-limit";
            else if(measurement <= yellowHighLimit) alertLevel = "yellow-high-limit";
            else if(measurement <= redHighLimit) alertLevel = "red-high-limit";
            else System.out.println("");
        }

        status.put("satelliteId", input[1]);
        status.put("severity", alertLevel);
        status.put("component", input[input.length-1]);
        status.put("timestamp",input[0]);
        return status;
    }

    public static void sortComponent(Map<String, String> status, ArrayList<Map<String,String>> TSTAT, ArrayList<Map<String,String>> BATT){
        if(status.containsValue("TSTAT")){
            TSTAT.add(status);
        }
        if(status.containsValue("BATT")){
            BATT.add(status);
        }
    }

    public static void checkTime(ArrayList<Map<String, String>> log){
        //Check if log is empty
        if(log.isEmpty()) return;
        //Get the Most recent time entry from the log.
        Map<String,String> newestEntry = log.get(log.size() -1);
        String newestEntryTimeString = newestEntry.get("timestamp");
        LocalDateTime newestEntryTime = LocalDateTime.parse(newestEntryTimeString, DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS"));

        //Compare the newest Time entry with the oldest time Entry.
        for(int x = 0; x < log.size() -1; x++){
            //Get the most recent time entry from the log.
            Map<String, String> oldestEntry = log.get(x);
            String oldestTimeString = oldestEntry.get("timestamp");
            LocalDateTime oldestEntryTime = LocalDateTime.parse(oldestTimeString, DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS"));

            if(Duration.between(oldestEntryTime, newestEntryTime).toMinutes() >= 5){
                log.remove(x);
            }
        }
    }

    public static ArrayList<Map<String, String>> alert(ArrayList<Map<String, String>> log){
        //Group entrants by satelliteId.
        Map<String, List<Map<String, String>>> groupedID = log.stream().collect(Collectors.groupingBy(stringStringMap -> stringStringMap.get("satelliteId")));
        //Return list
        ArrayList<Map<String, String>> alertList = new ArrayList<>();
        //Check each satellite by ID.
        for(Map.Entry<String, List<Map<String, String>>> entry : groupedID.entrySet()){
            //Bucket to hold Red alerts.
            ArrayList<Map<String, String>> limitCount = new ArrayList<>();
            entry.getValue().forEach(map -> {
                if(map.get("severity").equals("red-high-limit") && map.get("component").equals("TSTAT")){
                    limitCount.add(map);
                }else if(map.get("severity").equals("red-low-limit") && map.get("component").equals("BATT")){
                    limitCount.add(map);
                }
            });
            if(limitCount.size() >= 3) alertList.add(limitCount.get(0));
        }
        return alertList;
    }


}

    /*
    Alert Threshold

    Term threshold is being used to describe boundary between 2 sections such as Red-high or Yellow High.
    Every threshold number gives x + 1 outcomes. Example: threshold 5: x > 5 = high , x < 5 = Low
    Having 4 thresholds should warrant 5 alerting signals.
    */