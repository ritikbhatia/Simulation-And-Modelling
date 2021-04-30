package simulation;

/**
 * The interface {@code Event} for all events in the simulation
 */
public interface Event {

    // Get the event time
    public double getTime();

    // Get the current station
    public Station getcurrStation();

}