// routine to handle call termination event
public void handleCallTermination(CallTerminationEvent E) {

    // synchronize simulation clock
    simClock = E.time;

    // retrieve current base station
    BaseStation currStation = E.getBaseStation();
    
    // release the channel used since car has exited the system
    currStation.releaseChannel();
}