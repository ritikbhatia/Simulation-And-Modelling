package simulation;

/**
 * The class {@code Station} for the base station in the simulation
 */
public class Station {

    private int stationId;
    private int numAvailChannels;
    private int numberOfReservations;

    // Constructor
    public Station(int stationId, int numAvailChannels, int numberOfReservations) {
        this.stationId = stationId;
        this.numAvailChannels = numAvailChannels;
        this.numberOfReservations = numberOfReservations;
    }

    // Get the station id
    public int getStationId() {
        return stationId;
    }

    // Get the number of available channels
    public int getnumAvailChannels() {
        return numAvailChannels;
    }

    // Get the boolean value for call handover reservation
    public int getNumReserved() {
        return numberOfReservations;
    }

    // Acquire an available channel in the station
    public void acquireAnAvailableChannel() {
        this.numAvailChannels--;
    }

    // Release an acquired channel
    public void releaseAnAcquiredChannel() {
        this.numAvailChannels++;
    }
}