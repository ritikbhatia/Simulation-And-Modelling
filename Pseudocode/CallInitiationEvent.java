// routine to handle call initiation event
void handleCallInitiation(CallInitiationEvent E) {
        
        // synchronize simulation clock with time of new event
        simClock = E.time;

        // get current base station in which the car is
        BaseStation currStation = E.getBaseStation();

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

        // if the car reached next base station before the call ended, and the next base station exists
        // car is moving towards base station 20
        if (E.getCallDuration() > remainingTime && E.getDirection() == Direction.ToLastStation && currStation.getId() != 20) {
                // create a new CallHandoverEvent
                nextEvent = new CallHandoverEvent(E.id, E.getDirection(), newEventTime, E.getSpeed(), currStation, durationOfNewEvent);
        }
        // if the car reached next base station before the call ended, and the next base station exists
        // car is moving towards station 1 
        else if (E.getCallDuration() > remainingTime && E.getDirection() == Direction.ToFirstStation && currStation.getId() != 1) {
                nextEvent = new CallHandoverEvent(E.id, E.getDirection(), newEventTime, E.getSpeed(), currStation, durationOfNewEvent);
        }
        // call ended before entering the next station
        else {
                // create a new CallTerminationEvent
                nextEvent = new CallTerminationEvent(E.id, E.getBaseStation(), newEventTime);
        }
        
        // add the next event in the Future Event List (FEL), scheduled at time newEventTime
        eventQueue.add(nextEvent);
        

        /////////////////////////////  Assigning channel to the call /////////////////////////////
        
        // check if the base station can take a new call
        // canHandleCall() function returns values that depends on the type of FCA scheme used
        if (currStation.canHandleCall()) {
                // use 1 channel of the base station
                // equivalent to reducing number of available channels in the base station
                currStation.assignChannel();
        } else {
                // if no free channels available in the base station, block the call
                numCallsBlocked++;
        }
}
