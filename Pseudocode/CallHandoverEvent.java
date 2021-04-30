void handleCallHandover(CallHandoverEvent E) {

        // synchronize simulation clock
        simClock = E.time;

        // increment number of handovers
        numCallsHandover++;
        
        // get the current base station
        BaseStation currStation = E.getBaseStation();

        /////////////////////////////  Assigning channel to the call /////////////////////////////

        // variable for the next base station
        BaseStation nextStation;

        // if car is moving towards base station 20
        if (E.getDirection() == Direction.ToLastStation) {
                nextStation = baseStations[currStation.getId() + 1];
        } else {
                nextStation = baseStations[currStation.getId() - 1];
        }
        
        // release the channel used in the base station earlier
        currStation.releaseChannel();

        // can the next base station handle the call that is handed over
        // the logic for canHandleCall depends on the type of FCA scheme being used
        if (nextStation.canHandleCall()) {
                nextStation.assignChannel();
        } else {
                // if the next base station cannot handle the call, then drop the call
                // increment counter for number of calls dropped
                numCallsDropped++;
        }

        ///////////////////////////// Schedule Next Event /////////////////////////////
        
        // get distance remaining before car enters next base station
        // current position is random value between 0 and 2000, within the base station
        double nextStationDistance = 2000 - E.getCurrentPosition();
        
        // calculate time remaining before car enters next base station
        double timeRemaining = nextStationDistance / E.getSpeed();

        // calculate new event time
        double newEventTime = simClock + remainingTime;

        // calculate the new event duration
        double durationOfNewEvent = E.getCallDuration() - timeRemaining;

        // variable to queue next event in Future Event List
        Event nextEvent;

        // if the car reached next station before the call ended, and the next base station exists
        // and car is moving towards base station 20
        if (currStation.getId() != 20 && E.getCallDuration() > remainingTime && E.getDirection() == Direction.ToLastStation) {
                // create a new CallHandoverEvent
                nextEvent = new CallHandoverEvent(E.id, E.getDirection(), newEventTime, E.getSpeed(), currStation, durationOfNewEvent);
        }
        // if the car reached next station before the call ended, and the next base station exists
        // and car is moving towards station 1
        else if (currStation.getId() != 1 && E.getCallDuration() > remainingTime && E.getDirection() == Direction.ToFirstStation) {
                nextEvent = new CallHandoverEvent(E.id, E.getDirection(), newEventTime, E.getSpeed(), currStation, durationOfNewEvent);
        }
        // call ended before entering the next station
        else {
                // create a new CallTerminationEvent
                nextEvent = new CallTerminationEvent(E.id, E.getBaseStation(), newEventTime);
        }
        
        // add the next event in the Future Event List (FEL), scheduled at time newEventTime
        eventQueue.add(nextEvent);
}
