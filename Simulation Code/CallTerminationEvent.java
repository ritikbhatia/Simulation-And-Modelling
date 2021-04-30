package simulation;

/**
 * The class {@code CallTerminationEvent} for the call termination events
 */
public class CallTerminationEvent implements Event {

    private double time;
    private Station currStation;

    // Constructor
    public CallTerminationEvent(double time, Station currStation) {
        this.time = time;
        this.currStation = currStation;
    }

    @Override
    public double getTime() {
        return time;
    }

    @Override
    public Station getcurrStation() {
        return currStation;
    }
}