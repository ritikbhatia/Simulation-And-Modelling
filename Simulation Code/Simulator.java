package simulation;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * The class {@code Simulator} to run the simulation
 */
public class Simulator {

    private static final int TOT_CALLS = 250000;
    private static final int NUM_WARMUP_CALLS = 100000;
    private static final int SCALE = 5;
    private static final int NUMBER_OF_AVAILABLE_CHANNELS = 10;
    private static final Comparator<Event> COMPARATOR = Comparator.comparing(Event::getTime);
    private static final String OUTPUT_FILE = "output.csv";

    private double clock;
    private int numberOfReservations;
    private int generatedCalls;
    private int numCallsBlocked;
    private int numberOfDroppedCalls;
    private PriorityQueue<Event> fel;
    private List<Station> stations;
    private List<List<String>> statistics;

    // Constructor
    public Simulator(int numberOfReservations) {
        this.clock = 0;
        this.numberOfReservations = numberOfReservations;
        this.generatedCalls = 0;
        this.numCallsBlocked = 0;
        this.numberOfDroppedCalls = 0;
        this.fel = new PriorityQueue<>(1, COMPARATOR);
        this.stations = new ArrayList<>();
        this.statistics = new ArrayList<>();
    }

    // Start the simulator
    public void start() {
        // Create 20 base stations, each with 10 available channels and given FCA Scheme
        for (int i = 0; i < 20; i++) {
            stations.add(new Station(i + 1, NUMBER_OF_AVAILABLE_CHANNELS, numberOfReservations));
        }

        // Generate the first initiation record data
        CallInitiationEvent event = generateInitiationEvent();
        // Add the event to FEL
        fel.add(event);
        // Calculate the statistic when a call is generated
        calcStats();
        // Start the event handling routine
        handleEvent();
    }

    // Generate statistics report
    public void generateStatisticsReport() {
        int totalNumberOfCalls = TOT_CALLS - NUM_WARMUP_CALLS;
        BigDecimal blockedCallsRate = BigDecimal.valueOf(numCallsBlocked).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalNumberOfCalls), SCALE, RoundingMode.HALF_UP);
        BigDecimal droppedCallsRate = BigDecimal.valueOf(numberOfDroppedCalls).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalNumberOfCalls), SCALE, RoundingMode.HALF_UP);

        // Print statistics
        System.out.println("FCA Scheme: "
                + (numberOfReservations > 0 ? "HANDOVER RESERVATION " + numberOfReservations : "NO RESERVATION"));
        System.out.println("Number of Warm Up Calls: " + NUM_WARMUP_CALLS);
        System.out.println("Total number of Calls (after Warm Up period): " + totalNumberOfCalls);
        System.out.println("Number of Blocked Calls: " + numCallsBlocked);
        System.out.println("Number of Dropped Calls: " + numberOfDroppedCalls);
        System.out.println("Blocked Calls Rate (%): " + blockedCallsRate);
        System.out.println("Dropped Calls Rate (%): " + droppedCallsRate);

        // Write the statistics to output file
        try {
            String filePath = System.getProperty("user.dir") + "/" + OUTPUT_FILE;
            FileWriter writer = new FileWriter(filePath, true);

            // Uncomment the section below to output the changes in
            // blocked calls and dropped calls rate over the number of calls
            // (need to uncomment "import Collectors" above)

            // for (List<String> statistic : statistics) {
            // String collect = statistic.stream().collect(Collectors.joining(","));
            // writer.write(collect);
            // writer.write("\n");
            // }
            // writer.close();

            writer.write(Integer.toString(numberOfReservations) + ',');
            writer.write(blockedCallsRate.toString() + ',' + droppedCallsRate.toString());
            writer.write("\n");
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // Event handling routine
    private void handleEvent() {
        // Handle events from FEL
        while (!fel.isEmpty()) {
            // Get the event from FEL
            Event event = fel.remove();
            // Clock synchronization
            clock = event.getTime();
            // System.out.println(clock);
            // Handle each type of event
            if (event instanceof CallInitiationEvent) {
                handleCallInitiationEvent((CallInitiationEvent) event);
            } else if (event instanceof CallHandoverEvent) {
                handleCallHandoverEvent((CallHandoverEvent) event);
            } else if (event instanceof CallTerminationEvent) {
                handleCallTerminationEvent((CallTerminationEvent) event);
            }

            // Reset after the warm up period
            if (generatedCalls == NUM_WARMUP_CALLS) {
                numCallsBlocked = 0;
                numberOfDroppedCalls = 0;
            }
        }
    }

    // Handle CallInitiationEvent
    private void handleCallInitiationEvent(CallInitiationEvent event) {
        // Clock synchronization
        clock = event.getTime();
        // Get current station
        Station currStation = event.getcurrStation();
        // Get car speed
        double carSpeed = event.getCarSpeed();
        // Get car position
        double carPosition = event.getCarPos();
        // Get call duration
        double callDuration = event.getCallDuration();
        // Get car direction
        Direction carDirection = event.getCarDirection();

        // Check for an available channel for Call Initiation event
        int numAvailChannels = currStation.getnumAvailChannels();
        /*
         * The Call Initiation event is blocked if: the number of available channels
         * less than or equal to reserved channels
         */
        if (numAvailChannels <= currStation.getNumReserved()) {
            // Increase the number of blocked calls
            numCallsBlocked++;
        } else {
            // Acquire an available channel
            currStation.acquireAnAvailableChannel();

            // Get the station id
            int stationId = currStation.getStationId();
            // Calculate distance to next station (km)
            double distanceToNextStation = 2 - carPosition;
            // Calculate time to next station (sec)
            double timeToNextStation = (distanceToNextStation / carSpeed) * 3600;

            // Initialize next event
            Event nextEvent;
            // Create Call Termination event if:
            // 1. call duration is less than or equal to the time to next station
            if (callDuration <= timeToNextStation) {
                // Calculate termination time
                double terminationTime = clock + callDuration;
                // Create a Call Termination event
                nextEvent = new CallTerminationEvent(terminationTime, currStation);
            }
            // 2. call is in the last station, depending on the direction of the car
            else if ((carDirection == Direction.TO_20TH_STATION && stationId == 20)
                    || (carDirection == Direction.TO_1ST_STATION && stationId == 1)) {
                // Calculate termination time
                double terminationTime = clock + timeToNextStation;
                // Create a Call Termination event
                nextEvent = new CallTerminationEvent(terminationTime, currStation);
            }
            // create Call Handover event otherwise
            else {
                // Calculate Handover time
                double handoverTime = clock + timeToNextStation;
                // Calculate call remaining duration
                double callRemainingDuration = callDuration - timeToNextStation;
                // Create a Call Handover event
                nextEvent = new CallHandoverEvent(handoverTime, currStation, carSpeed, callRemainingDuration,
                        carDirection);
            }

            // Add the next event to FEL
            fel.add(nextEvent);
        }

        if (generatedCalls < TOT_CALLS) {
            Event nextCallInitiation = generateInitiationEvent();
            // Add the new initiation event to FEL
            fel.add(nextCallInitiation);
            // Calculate statistics again
            calcStats();
        }
    }

    // Handle CallHandoverEvent
    private void handleCallHandoverEvent(CallHandoverEvent event) {
        // Clock synchronization
        clock = event.getTime();
        // Get current station
        Station currStation = event.getcurrStation();
        // Get car speed
        double carSpeed = event.getCarSpeed();
        // Get call duration
        double callDuration = event.getCallDuration();
        // Get car direction
        Direction carDirection = event.getCarDirection();

        // Release the previously acquired channel
        currStation.releaseAnAcquiredChannel();
        // Update the current station
        if (carDirection == Direction.TO_20TH_STATION) {
            currStation = stations.get(currStation.getStationId());
        } else {
            currStation = stations.get(currStation.getStationId() - 2);
        }

        // Check for an available channel for Call Initiation event
        int numAvailChannels = currStation.getnumAvailChannels();
        // The Call Handover event is dropped if there is no available channel
        // regardless of the FCA scheme
        if (numAvailChannels == 0) {
            // Increase the number of dropped calls
            numberOfDroppedCalls++;
            // Exit the handling function
            return;
        } else {
            // Acquire an available channel
            currStation.acquireAnAvailableChannel();
        }

        // Get the station id
        int stationId = currStation.getStationId();
        // Distance to the next station in a handover event is alwyas 2 km
        double distanceToNextStation = 2;
        // Calculate time to next station (sec)
        double timeToNextStation = (distanceToNextStation / carSpeed) * 3600;

        // Initialize next event
        Event nextEvent;
        // Create Call Termination event if:
        // 1. call duration is less than or equal to the time to next station
        if (callDuration <= timeToNextStation) {
            // Calculate termination time
            double terminationTime = clock + callDuration;
            // Create a Call Termination event
            nextEvent = new CallTerminationEvent(terminationTime, currStation);
        }
        // 2. call is in the last station, depending on the direction of the car
        else if ((carDirection == Direction.TO_20TH_STATION && stationId == 20)
                || (carDirection == Direction.TO_1ST_STATION && stationId == 1)) {
            // Calculate termination time
            double terminationTime = clock + timeToNextStation;
            // Create a Call Termination event
            nextEvent = new CallTerminationEvent(terminationTime, currStation);
        }
        // create Call Handover event otherwise
        else {
            // Calculate Handover time
            double handoverTime = clock + timeToNextStation;
            // Calculate call remaining duration
            double callRemainingDuration = callDuration - timeToNextStation;
            // Create a Call Handover event
            nextEvent = new CallHandoverEvent(handoverTime, currStation, carSpeed, callRemainingDuration, carDirection);
        }

        // Add the next event to FEL
        fel.add(nextEvent);
    }

    // Handle CallTerminationEvent
    private void handleCallTerminationEvent(CallTerminationEvent event) {
        // Clock synchronization
        clock = event.getTime();
        // Get current station
        Station currStation = event.getcurrStation();
        // Release the previously acquired channel
        currStation.releaseAnAcquiredChannel();
    }

    // Generate a Call Initiation event
    private CallInitiationEvent generateInitiationEvent() {
        // Get the initiation time
        double time = clock + RandomNumberGenerator.getInterArrivalTime();
        // Get the current station
        int stationId = RandomNumberGenerator.getBaseStation();
        Station currStation = stations.get(stationId - 1);
        // Get the car speed
        double carSpeed = RandomNumberGenerator.getCarSpeed();
        // Get the car position
        double carPosition = RandomNumberGenerator.getCarPos();
        // Get the call duration
        double callDuration = RandomNumberGenerator.getCallDuration();
        // Get the car direction
        Direction carDirection = RandomNumberGenerator.getCarDirection();

        // Generate the first call initiation event
        CallInitiationEvent event = new CallInitiationEvent(time, currStation, carSpeed, carPosition, callDuration,
                carDirection);

        // Increase the number of generated calls
        generatedCalls++;

        return event;
    }

    // Calculate statistic and add to the statistics list
    private void calcStats() {
        List<String> statistic = new ArrayList<>();
        BigDecimal blockedCallsRate = BigDecimal.valueOf(numCallsBlocked).divide(BigDecimal.valueOf(generatedCalls),
                SCALE, RoundingMode.HALF_UP);
        BigDecimal droppedCallsRate = BigDecimal.valueOf(numberOfDroppedCalls)
                .divide(BigDecimal.valueOf(generatedCalls), SCALE, RoundingMode.HALF_UP);

        statistic.add(blockedCallsRate.toString());
        statistic.add(droppedCallsRate.toString());

        statistics.add(statistic);
    }
}