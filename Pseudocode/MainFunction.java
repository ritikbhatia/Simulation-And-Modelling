// routine to handle the main function of the simulation
void MainFunction() {

        ///////////////////////// Initialize Simulation Variables //////////////////////////
        
        Queue<Event> eventQueue = new Queue<Event>();
        
        // set simulation clock to start at 0
        double simClock = 0;

        // total number of calls = 10000 (from excel sheet) + number of warmup calls
        int totCalls = 10700;

        // fix number of warmup calls at 700
        // regardless of FCA scheme (for pseudocode purposes)
        // to be considered later
        int numCallsWarmup = 700;

        // total calls without warmup
        int totCallsWithoutWarmup = totCalls - numCallsWarmup;

        // initialize number of calls dropped, blocked and handed over, to 0
        int numCallsDropped = 0;
        int numCallsBlocked = 0;
        int numCallsHandover = 0;

        // read the data from the Excel sheet provided
        Event[] data = getExcelFileData();

        // queue to store all initiation events
        // total 10000 calls initiated
        Queue<Event> callInitiationEvents = new Queue<Event>(); 

        // create a base station list
        BaseStation[] baseStations = new BaseStation[20];

        // initialize list of base stations by creating objects
        for(int num = 0; num < 20; num++) {
                baseStations[num] = new BaseStation(num);
        }

        // iterate over entries in the excel file
        for(int row = 0; row < data.length; row++) {
                // generate random car direction
                // using ternary operator ? and :
                Direction direction = Math.random() >= 0.5 ? Direction.ToLastStation : Direction.ToFirstStation;

                Event currData = data[row];

                // for each entry in the excel file, create a CallInitiationEvent
                CallInitiationEvent event = new CallInitiationEvent(currData.id, direction, currData.time, currData.speed, currData.station, currData.duration);
                callInitiationEvents.add(event);
        }

        ///////////////////////// Perform Events in Future Event List //////////////////////////

        // process till the Future Event List (FEL), that is, the eventQueue, is not empty
        while(callInitiationEvents.length > 0 || !eventQueue.isEmpty()) {

                // extract first element to be processed
                Event eventToProcess;

                // if no more events in eventQueu
                if(eventQueue.isEmpty()){
                        // will process a call initiation event
                        eventToProcess = callInitiationEvents.peek();
                        callInitiationEvents.remove();
                }
                // if a call initiation event is scheduled before an event in the event queue
                else if(eventQueue.peek().time > callInitiationEvents.peek().time){
                        // will process a call initiation event
                        eventToProcess = callInitiationEvents.peek();  
                        callInitiationEvents.remove();
                }
                // if another event is scheduled before a call initiation event
                else if(eventQueue.peek().time < callInitiationEvents.peek().time){
                        eventToProcess = eventQueue.peek();
                        eventQueue.remove();
                }


                // if Call Initiation Event
                if (eventToProcess instanceof CallInitiationEvent) {
                        handleCallInitiation(eventToProcess);
                }
                // if Call Handover Event 
                else if (eventToProcess instanceof CallHandoverEvent) {
                        handleCallHandover(eventToProcess);
                }
                // if Call Termination Event 
                else if (eventToProcess instanceof CallTerminationEvent) {
                        handleCallTermination(eventToProcess);
                }
        }

        // calculate results to know whether Quality of Service (QoS) reached
        double percentageCallsBlocked = (numCallsBlocked / totCallsWithoutWarmup) * 100;
        double percentageCallsDropped = (numCallsDropped / totCallsWithoutWarmup) * 100;

        // generate the statistics report
        generateStatReport();
}
