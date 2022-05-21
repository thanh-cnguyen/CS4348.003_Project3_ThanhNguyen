
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ScheduleAlgorithm {
    HashMap<String, String> scheduleInfoList = new HashMap<>(); // To store algorithm components
    ArrayList<String[]> processEventList = new ArrayList<>(); // To store processes obtained from the process file
    ArrayList<Process> processObjList = new ArrayList<>(); // To store objects of process statistics
    ArrayList<Process> finishList = new ArrayList<>(); // List of finished processes
    Queue<Process> eventQueue = new PriorityQueue<>(); // Priority queue of events, sorted by time stamp
    Queue<Process> readyQueue = new PriorityQueue<>(); // Queue for holding processes when CPU is busy
    Queue<Process> updateQueue = new LinkedList<>(); // Queue for updating process priorities

    boolean noExecution = true; // Flag for available CPU accessibility
    boolean programTerminate = false; // Flag for process scheduling stimulation
    boolean quantumProcessed = false; // Flag to mark the process activity will be handled using quantum
    boolean serviceGiven = false;
    int i = 0; // process id starts from 1
    int processCount = 0; // Number of executing processes
    int currentTime = 0; // Current time stamp
    int quantum = Integer.MAX_VALUE;
    double alpha = 0.0; // The weight factor in exponential averaging
    Process.Activity currActivity; // Process current activity
    Process.Activity readyActivity; // Process ready-to-run activity

    public ScheduleAlgorithm(String[] scheduleFile) throws FileNotFoundException {
        String scheduleDirectory = "schedule_files/";
        File fileName = new File(scheduleDirectory + scheduleFile[0] + "." + scheduleFile[1]);
        Scanner sc = new Scanner(fileName);
        while (sc.hasNext()) {
            String[] input = sc.nextLine().split("=", 2);
            scheduleInfoList.put(input[0], input[1]);
        }
    }

    public void getProcesses(String[] processFile) throws FileNotFoundException {
        String processDirectory = "process_files/";
        File fileName = new File(processDirectory + processFile[0] + "." + processFile[1]);
        Scanner sc = new Scanner(fileName);
        while (sc.hasNextLine()) { // 1. Load processes from process file.
            processEventList.add(sc.nextLine().split(" ", 2));
        }
        processCount = processEventList.size();
    }

    public boolean noProcessLeft() {
        return eventQueue.isEmpty() && readyQueue.isEmpty();
    }

    public void printRes() {
        double meanTurnaroundTime = 0;
        double meanNormalizedTurnaroundTime = 0;
        double meanAvgResTime = 0;
        finishList.sort(Comparator.comparing(Process::getId));
        for (Process a : finishList) {
            System.out.println(a.toString());
            meanTurnaroundTime += a.getTurnaroundTime();
            meanNormalizedTurnaroundTime += a.getNormalizedTurnaroundTime();
            meanAvgResTime += a.getAverageResponseTime();
        }
        System.out.println("\nMean Turnaround: " + (meanTurnaroundTime / processCount));
        System.out.println("Mean Normalized Turnaround: " + (meanNormalizedTurnaroundTime / processCount));
        System.out.println("Mean Average Response Time: " + (meanAvgResTime / processCount));
    }

    public void initializeEventQueue() {
        for (String[] a : processEventList) {
            processObjList.add(new Process((i + 1), Integer.parseInt(a[0]), scheduleInfoList.get("name"), a[1])); // Record arrival time
            processObjList.get(i).setCurrEventCode(Process.Event.ARRIVE);
            // Initialize event queue with the arrival events for the processes
            eventQueue.add(processObjList.get(i));
            i++;
        }
    }

    public void updateEventQueue() {
        while (!eventQueue.isEmpty()) {
            updateQueue.add(eventQueue.remove());
        }

        while (!updateQueue.isEmpty()) {
            eventQueue.add(updateQueue.remove());
        }
    }

    public void fcfsHandler() {
        System.out.println("First Come First Serve Simulation...");
        initializeEventQueue(); // Load up events
        System.out.println("\n------------HEAD OF RUN LOG------------");
        while (!programTerminate) {
            Process currProcess = eventQueue.remove();
            Process.Event event = currProcess.getCurrEventCode();
            currentTime = currProcess.getTimeStamp();
            currActivity = currProcess.getCurrActivity();
            switch (event) {
                case ARRIVE: // Process CPU and null activities
                    if (noExecution) {
                        System.out.println("Process " + currProcess.getId() + " arrives to use CPU (＾O＾)");
                        noExecution = false; // Set CPU is busy

                        if (currActivity.code == null || currActivity.code == Process.ActivityCode.IO) {
                            currProcess.getNextCurrActivity(); // Update Current Activity to use CPU
                            currActivity = currProcess.getCurrActivity();
                        }
                        if (currProcess.getStartTime() == -1)
                            currProcess.setStartTime(currentTime); // Set the first time Process gains control of the  CPU

                        if (currProcess.getWaitingTimestamp() < 0) { // Capture wait time for CPU activities
                            currProcess.addAverageResponseTime(currentTime - currProcess.getArrivalTime());
                        } else
                            currProcess.addAverageResponseTime(currentTime - currProcess.getWaitingTimestamp());

                        if (currActivity.code == Process.ActivityCode.CPU) {
                            currentTime += currActivity.duration;
                            currProcess.setTimeStamp(currentTime); // Record time stamp for current process
                            currProcess.addServiceTime(currActivity.duration); // Sum all CPU possessed times for service time
                        } else {
                            System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                            System.exit(1);
                        }

                        if (currProcess.eventIsLast()) { // Exit if last activity is being processed
                            currProcess.setCurrEventCode(Process.Event.EXIT);
                        } else
                            currProcess.setCurrEventCode(Process.Event.BLOCK);

                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    break;
                case BLOCK: // Process IO
                    System.out.println("Process " + currProcess.getId() + " being blocked ¯\\_(ツ)_/¯");
                    if (currActivity.code == Process.ActivityCode.CPU) {
                        currProcess.getNextCurrActivity(); // Update Current Activity to use IO devices
                        currActivity = currProcess.getCurrActivity();
                    }
                    int wait = currentTime + currActivity.duration;
                    currProcess.setWaitingTime(wait); // Set wait time for current activity
                    System.out.println("Process " + currProcess.getId() + " will be ready at " + wait + " units of time");
                    if (currActivity.code == Process.ActivityCode.IO)
                        currProcess.setTimeStamp(currentTime + currActivity.duration); // Record time stamp for current process
                    else {
                        System.out.println("Block: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }
                    currProcess.setCurrEventCode(Process.Event.UNBLOCK);
                    eventQueue.add(currProcess); // Add back to move to next event
                    if (!readyQueue.isEmpty()) { // Check for any waiting processes
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Block: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Redirect to Event queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
                case UNBLOCK:
                    System.out.println("Unblock: Releasing Process " + currProcess.getId() + " at " + currentTime + " units of time");

                    if (currProcess.eventIsLast()) // Check for any CPU accessing after IO usage
                        currProcess.setCurrEventCode(Process.Event.EXIT);
                    else if (currActivity.code == Process.ActivityCode.IO) {
                        currProcess.getNextCurrActivity();
                        currProcess.setCurrEventCode(Process.Event.ARRIVE);
                    } else {
                        System.out.println("Unblock: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }

                    if (noExecution) {
                        System.out.println("Releasing Process " + currProcess.getId());
                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else {
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    }
                    break;
                case EXIT:
                    currProcess.setFinishTime(currentTime); // Set Finish time
                    currProcess.setTurnaroundTime(); // Set Turnaround time by subtracting finish time by arrival time
                    currProcess.setNormalizedTurnaroundTime(); // Set Normalized Turnaround time
                    currProcess.setAverageResponseTime(); // Set Average Response Time
                    System.out.println("Process " + currProcess.getId() + " finishes.");
                    finishList.add(currProcess); // Add to completed List
                    if (!readyQueue.isEmpty() && !noExecution) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Exit: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Redirect to Event queue to process
                    }
                    noExecution = true; // Set CPU is free
                    if (noProcessLeft()) // Check to terminate simulation
                        programTerminate = true;
                    break;
                case TIMEOUT:
                    System.out.println("TIMEOUT: Process " + currProcess.getId() + " overused CPU resources.");
            }
        }
        System.out.println("-------------END OF RUN LOG-------------");
        System.out.println("\nFinish FCFS in " + currentTime + " units of time.");
        printRes();
    }

    public void vrrHandler() {
        System.out.println("Virtual Round Robin Simulation...");
        initializeEventQueue();
        quantum = Integer.parseInt(scheduleInfoList.get("quantum"));
        System.out.println("Quantum=" + quantum);
        System.out.println("------------HEAD OF RUN LOG------------");
        int quantumCurrTime = 0;
        while (!programTerminate) {
            Process currProcess = eventQueue.remove();
            Process.Event event = currProcess.getCurrEventCode();
            currentTime = currProcess.getTimeStamp();
            currActivity = currProcess.getCurrActivity();
            switch (event) {
                case ARRIVE: // Process CPU and null activities
                    if (noExecution) {
                        System.out.println("Process " + currProcess.getId() + " arrives to use CPU (＾O＾)");
                        noExecution = false; // Set CPU is busy

                        if (currActivity.code == null || currActivity.code == Process.ActivityCode.IO) {
                            currProcess.getNextCurrActivity(); // Update Current Activity to use CPU
                            currActivity = currProcess.getCurrActivity();
                        }

                        if (currProcess.getStartTime() == -1) { // Set the first time Process gains control of the  CPU
                            currProcess.setStartTime(quantumCurrTime);
                        }

                        if (eventQueue.isEmpty()) { // Synchronize quantum current time and regular current time
                            quantumCurrTime = currentTime;
                        }

                        if (currProcess.getWaitingTimestamp() < 0) { // Capture wait time for CPU activities
                            currProcess.addAverageResponseTime(quantumCurrTime - currProcess.getArrivalTime());
                        } else
                            currProcess.addAverageResponseTime(quantumCurrTime - currProcess.getWaitingTimestamp());

                        if (currActivity.duration > quantum) {
                            currProcess.setCurrEventCode(Process.Event.TIMEOUT);
                            quantumProcessed = true;
                        } else {
                            quantumProcessed = false;

                            if (currActivity.code == Process.ActivityCode.CPU) {
                                quantumCurrTime += currActivity.duration;
                                currentTime += currActivity.duration;
                                // for which time is up-to-date
                                // Record time stamp for current process
                                currProcess.setTimeStamp(Math.max(quantumCurrTime, currentTime)); // Record time stamp for current process
                                currProcess.addServiceTime(currActivity.duration); // Sum all CPU possessed times for service time
                            } else {
                                System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                                System.exit(1);
                            }

                            if (currProcess.eventIsLast()) { // Exit if last activity is being processed
                                currProcess.setCurrEventCode(Process.Event.EXIT);
                            } else
                                currProcess.setCurrEventCode(Process.Event.BLOCK);
                        }
                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    break;
                case BLOCK: // Process IO
                    System.out.println("Process " + currProcess.getId() + " being blocked ¯\\_(ツ)_/¯");
                    if (currActivity.code == Process.ActivityCode.CPU) {
                        currProcess.getNextCurrActivity();
                        currActivity = currProcess.getCurrActivity();
                    }
                    int wait = currentTime + currActivity.duration;
                    currProcess.setWaitingTime(wait);
                    quantumCurrTime = currentTime;
                    System.out.println("Process " + currProcess.getId() + " will be ready at " + wait + " units of time");
                    if (currActivity.code == Process.ActivityCode.IO)
                        currProcess.setTimeStamp(wait);
                    else {
                        System.out.println("Block: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }
                    currProcess.setCurrEventCode(Process.Event.UNBLOCK);
                    eventQueue.add(currProcess); // Add back to move to next event
                    if (!readyQueue.isEmpty()) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Block: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
                case UNBLOCK:
                    System.out.println("Unblock: Releasing Process " + currProcess.getId() + " at " + currentTime + " units of time");

                    if (currProcess.eventIsLast()) // Check for any CPU accessing after IO usage
                        currProcess.setCurrEventCode(Process.Event.EXIT);
                    else if (currActivity.code == Process.ActivityCode.IO) {
                        currProcess.getNextCurrActivity();
                        currProcess.setCurrEventCode(Process.Event.ARRIVE);
                    } else {
                        System.out.println("Unblock: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }

                    if (noExecution) {
                        System.out.println("Releasing Process " + currProcess.getId());
                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else {
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    }
                    break;
                case EXIT:
                    currProcess.setFinishTime(currentTime); // Set Finish time
                    currProcess.setTurnaroundTime(); // Set Turnaround time by subtracting finish time by arrival time
                    currProcess.setNormalizedTurnaroundTime(); // Set Normalized Turnaround time
                    currProcess.setAverageResponseTime(); // Set Average Response Time
                    System.out.println("Process " + currProcess.getId() + " finishes.");
                    finishList.add(currProcess); // Add process to completed list
                    if (!readyQueue.isEmpty() && !noExecution) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Exit: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    if (noProcessLeft()) // Check to terminate simulation
                        programTerminate = true;
                    break;
                case TIMEOUT:
                    if (quantumProcessed) {
                        currActivity.duration -= quantum;
                        currProcess.updateActivityQueueHead(currActivity);
                        currProcess.getNextCurrActivity(); // Update quantum Current Activity to use CPU

                        if (currActivity.code == Process.ActivityCode.CPU) {
                            quantumCurrTime += quantum; // Update quantum time
                            currProcess.setWaitingTime(quantumCurrTime);
                            currProcess.setTimeStamp(quantumCurrTime); // Record time stamp for current process
                            currProcess.addServiceTime(quantum); // Sum all CPU possessed times for service time
                        } else {
                            System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                            System.exit(1);
                        }

                        currProcess.setCurrEventCode(Process.Event.ARRIVE);
                        eventQueue.add(currProcess);
                    } else {
                        System.err.println("Timeout: Wrong Event Code" + currProcess.getCurrEventCode());
                        System.exit(1);
                    }
                    if (!readyQueue.isEmpty()) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Timeout: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
            }
        }
        System.out.println("-------------END OF RUN LOG-------------");
        System.out.println("\nFinish VRR in " + currentTime + " units of time.");
        printRes();
    }

    public void initializeTotalRemainingBurstTime(boolean serviceGiven, double alpha) {
        if (!serviceGiven) {
            System.out.println("Service_give=" + false);
            if (!(0 < alpha && alpha < 1)) {
                System.err.println("VRR: Invalid weight factor alpha - " + alpha);
                System.exit(1);
            } else { // Calculate the next predicted service times using alpha
                System.out.println("alpha=" + alpha);
                for (Process p : eventQueue) {
                    p.setInitCPUDuration(p.peakActivityQueue().duration); // Record initial CPU duration
                    p.setRemainingBurstTime((int) (alpha * (p.getInitCPUDuration()) + (1 - alpha) * (p.getInitCPUDuration())));
                }
            }
        } else {
            System.out.println("Service_give=" + true);
            for (Process p : eventQueue) {
                for (Process.Activity a : p.activityQueue) { // Get total CPU remaining Burst times using summation
                    if (a.code == Process.ActivityCode.CPU)
                        p.addRemainingBurstTime(a.duration);
                }
            }
        }
    }

    public void srtHandler() {
        System.out.println("Shortest Remaining Time Simulation...");
        initializeEventQueue(); // Load up events
        serviceGiven = Boolean.parseBoolean(scheduleInfoList.get("service_give"));
        alpha = Double.parseDouble(scheduleInfoList.get("alpha"));
        initializeTotalRemainingBurstTime(serviceGiven, alpha); // Get total burst time with service and/or alpha
        updateEventQueue(); // Set new priorities
        System.out.println("\n------------HEAD OF RUN LOG------------");
        int actualBurstTime;
        int potentialBurstTime = Integer.MAX_VALUE;
        while (!programTerminate) {
            Process currProcess = eventQueue.remove();
            Process nextProcess = eventQueue.peek();
            Process.Event event = currProcess.getCurrEventCode();
            currentTime = currProcess.getTimeStamp();
            currActivity = currProcess.getCurrActivity();
            switch (event) {
                case ARRIVE: // Process CPU and null activities
                    if (noExecution) {
                        System.out.println("Process " + currProcess.getId() + " arrives to use CPU (＾O＾)");
                        noExecution = false; // Set CPU is busy

                        if (currActivity.code == null || currActivity.code == Process.ActivityCode.IO) {
                            currProcess.getNextCurrActivity(); // Update Current Activity to use CPU
                            currActivity = currProcess.getCurrActivity();
                        }

                        if (currProcess.getStartTime() == -1) {
                            currProcess.setStartTime(currentTime); // Set the first time Process gains control of the  CPU
                        }

                        if (currProcess.getWaitingTimestamp() < 0) { // Capture wait time for CPU activities
                            currProcess.addAverageResponseTime(currentTime - currProcess.getArrivalTime());
                        } else
                            currProcess.addAverageResponseTime(currentTime - currProcess.getWaitingTimestamp());

                        if (nextProcess != null) {
                            potentialBurstTime = nextProcess.getTimeStamp() - currentTime;
                            actualBurstTime = currentTime + currActivity.duration;
                            if (potentialBurstTime > 0 && actualBurstTime > nextProcess.getTimeStamp()) {
                                currProcess.setCurrEventCode(Process.Event.TIMEOUT);
                                eventQueue.add(currProcess);
                                break;
                            }
                        }

                        if (currActivity.code == Process.ActivityCode.CPU) {
                            currentTime += currActivity.duration;
                            currProcess.setTimeStamp(currentTime); // Record time stamp for current process
                            currProcess.addServiceTime(currActivity.duration); // Sum all CPU possessed times for service time
                            currProcess.removeRemainingBurstTime(currActivity.duration);
                        } else {
                            System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                            System.exit(1);
                        }

                        if (currProcess.eventIsLast()) { // Exit if last activity is being processed
                            currProcess.setCurrEventCode(Process.Event.EXIT);
                        } else
                            currProcess.setCurrEventCode(Process.Event.BLOCK);

                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    break;
                case BLOCK: // Process IO
                    System.out.println("Process " + currProcess.getId() + " being blocked ¯\\_(ツ)_/¯");
                    if (currActivity.code == Process.ActivityCode.CPU) {
                        currProcess.getNextCurrActivity(); // Update Current Activity to use IO devices
                        currActivity = currProcess.getCurrActivity();
                    }
                    int wait = currentTime + currActivity.duration;
                    currProcess.setWaitingTime(wait); // Set wait time for current activity
                    System.out.println("Process " + currProcess.getId() + " will be ready at " + wait + " units of time");
                    if (currActivity.code == Process.ActivityCode.IO)
                        currProcess.setTimeStamp(currentTime + currActivity.duration); // Record time stamp for current process
                    else {
                        System.out.println("Block: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }
                    currProcess.setCurrEventCode(Process.Event.UNBLOCK);
                    eventQueue.add(currProcess); // Add back to move to next event
                    if (!readyQueue.isEmpty()) { // Check for any waiting processes
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Block: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Redirect to Event queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
                case UNBLOCK:
                    System.out.println("Unblock: Process " + currProcess.getId() + " at " + currentTime + " units of time");
                    if (currProcess.eventIsLast()) // Check for any CPU accessing after IO usage
                        currProcess.setCurrEventCode(Process.Event.EXIT);
                    else
                        currProcess.setCurrEventCode(Process.Event.ARRIVE);

                    if (currActivity.code == Process.ActivityCode.IO) {
                        currProcess.getNextCurrActivity();
                        if (!serviceGiven)
                            currProcess.setRemainingBurstTime((int) (alpha * (currActivity.duration) + (1 - alpha) * (currProcess.getInitCPUDuration())));
                    }
                    if (noExecution) {
                        System.out.println("Releasing Process " + currProcess.getId());
                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else {
                        System.out.println("Unblock: Process " + currProcess.getId() + " will be in Ready Queue");
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    }
                    break;
                case EXIT:
                    currProcess.setFinishTime(currentTime); // Set Finish time
                    currProcess.setTurnaroundTime(); // Set Turnaround time by subtracting finish time by arrival time
                    currProcess.setNormalizedTurnaroundTime(); // Set Normalized Turnaround time
                    currProcess.setAverageResponseTime(); // Set Average Response Time
                    System.out.println("Process " + currProcess.getId() + " finishes.");
                    finishList.add(currProcess); // Add to completed List
                    if (!readyQueue.isEmpty() && !noExecution) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Exit: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    if (noProcessLeft()) // Check to terminate simulation
                        programTerminate = true;
                    break;
                case TIMEOUT:
                    System.out.println("Timeout: Process " + currProcess.getId() + " adjusts CPU usage time.");
                    currActivity.duration -= potentialBurstTime;
                    currProcess.updateActivityQueueHead(currActivity);
                    currProcess.getNextCurrActivity();

                    if (currActivity.code == Process.ActivityCode.CPU) {
                        currentTime += potentialBurstTime; // Update quantum time
                        currProcess.setWaitingTime(currentTime);
                        currProcess.setTimeStamp(currentTime); // Record time stamp for current process
                        currProcess.addServiceTime(potentialBurstTime); // Sum all CPU possessed times for service time
                        if (!serviceGiven) {

                        } else {
                            currProcess.removeRemainingBurstTime(potentialBurstTime);
                        }
                    } else {
                        System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }

                    currProcess.setCurrEventCode(Process.Event.ARRIVE);
                    eventQueue.add(currProcess);
                    if (!readyQueue.isEmpty()) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Timeout: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
            }
        }
        System.out.println("-------------END OF RUN LOG-------------");
        System.out.println("\nFinish SRT in " + currentTime + " units of time.");
        printRes();
    }

    public void initializeResponseRatio(boolean serviceGiven, double alpha) {
        initializeTotalRemainingBurstTime(serviceGiven, alpha); // Get total burst time with service and/or alpha
        for (Process p : eventQueue) {
            p.calculateResponseRatio(p.getTimeStamp(), p.getRemainingBurstTime());
        }
        updateEventQueue(); // Set new priorities
    }

    public void hrrnHandler() {
        System.out.println("Highest Response Ratio Next Simulation...");
        initializeEventQueue(); // Load up events
        serviceGiven = Boolean.parseBoolean(scheduleInfoList.get("service_give"));
        alpha = Double.parseDouble(scheduleInfoList.get("alpha"));
        initializeResponseRatio(serviceGiven, alpha); // Get total burst time with service and/or alpha

        System.out.println("\n------------HEAD OF RUN LOG------------");
        while (!programTerminate) {
            Process currProcess = eventQueue.remove();
            Process.Event event = currProcess.getCurrEventCode();
            currentTime = currProcess.getTimeStamp();
            currActivity = currProcess.getCurrActivity();
            switch (event) {
                case ARRIVE: // Process CPU and null activities
                    if (noExecution) {
                        System.out.println("Process " + currProcess.getId() + " arrives to use CPU (＾O＾)");
                        noExecution = false; // Set CPU is busy

                        if (currActivity.code == null || currActivity.code == Process.ActivityCode.IO) {
                            currProcess.getNextCurrActivity(); // Update Current Activity to use CPU
                            currActivity = currProcess.getCurrActivity();
                        }
                        if (currProcess.getStartTime() == -1)
                            currProcess.setStartTime(currentTime); // Set the first time Process gains control of the  CPU

                        if (currProcess.getWaitingTimestamp() < 0) { // Capture wait time for CPU activities
                            currProcess.addAverageResponseTime(currentTime - currProcess.getArrivalTime());
                            currProcess.calculateResponseRatio(currentTime - currProcess.getWaitingTimestamp(), currProcess.getRemainingBurstTime());
                        } else {
                            currProcess.addAverageResponseTime(currentTime - currProcess.getWaitingTimestamp());
                            currProcess.calculateResponseRatio(currentTime - currProcess.getWaitingTimestamp(), currProcess.getRemainingBurstTime());
                        }

                        if (currActivity.code == Process.ActivityCode.CPU) {
                            currProcess.setTimeStamp(currentTime + currActivity.duration); // Record time stamp for current process
                            currProcess.addServiceTime(currActivity.duration); // Sum all CPU possessed times for service time
                            currProcess.removeRemainingBurstTime(currActivity.duration);
                        } else {
                            System.out.println("Arrive: Wrong Current Activity Code -- " + currActivity.code);
                            System.exit(1);
                        }


                        if (currProcess.eventIsLast()) { // Exit if last activity is being processed
                            currProcess.setCurrEventCode(Process.Event.EXIT);
                        } else
                            currProcess.setCurrEventCode(Process.Event.BLOCK);

                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    break;
                case BLOCK: // Process IO
                    System.out.println("Process " + currProcess.getId() + " being blocked ¯\\_(ツ)_/¯");
                    if (currActivity.code == Process.ActivityCode.CPU) {
                        currProcess.getNextCurrActivity(); // Update Current Activity to use IO devices
                        currActivity = currProcess.getCurrActivity();
                    }
                    int wait = currentTime + currActivity.duration;
                    currProcess.setWaitingTime(wait); // Set wait time for current activity
                    System.out.println("Process " + currProcess.getId() + " will be ready at " + wait + " units of time");
                    if (currActivity.code == Process.ActivityCode.IO)
                        currProcess.setTimeStamp(currentTime + currActivity.duration); // Record time stamp for current process
                    else {
                        System.out.println("Block: Wrong Current Activity Code -- " + currActivity.code);
                        System.exit(1);
                    }
                    currProcess.setCurrEventCode(Process.Event.UNBLOCK);
                    eventQueue.add(currProcess); // Add back to move to next event
                    if (!readyQueue.isEmpty()) { // Check for any waiting processes
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Block: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Redirect to Event queue to process
                    }
                    noExecution = true; // Set CPU is free
                    break;
                case UNBLOCK:
                    System.out.println("Unblock: Process " + currProcess.getId() + " at " + currentTime + " units of time");
                    if (currProcess.eventIsLast()) // Check for any CPU accessing after IO usage
                        currProcess.setCurrEventCode(Process.Event.EXIT);
                    else
                        currProcess.setCurrEventCode(Process.Event.ARRIVE);

                    if (currActivity.code == Process.ActivityCode.IO) {
                        currProcess.getNextCurrActivity();
                        if (!serviceGiven)
                            currProcess.setRemainingBurstTime((int) (alpha * (currActivity.duration) + (1 - alpha) * (currProcess.getInitCPUDuration())));
                    }
                    if (noExecution) {
                        System.out.println("Releasing Process " + currProcess.getId());
                        eventQueue.add(currProcess); // Redirect to Event queue to process
                    } else {
                        System.out.println("Unblock: Process " + currProcess.getId() + " will be in Ready Queue");
                        readyQueue.add(currProcess); // Redirect to Ready queue if a process is using CPU
                    }
                    break;
                case EXIT:
                    currProcess.setFinishTime(currentTime); // Set Finish time
                    currProcess.setTurnaroundTime(); // Set Turnaround time by subtracting finish time by arrival time
                    currProcess.setNormalizedTurnaroundTime(); // Set Normalized Turnaround time
                    currProcess.setAverageResponseTime(); // Set Average Response Time
                    System.out.println("Process " + currProcess.getId() + " finishes.");
                    finishList.add(currProcess); // Add to completed List
                    if (!readyQueue.isEmpty() && !noExecution) {
                        Process readyProcess = readyQueue.remove();
                        System.out.println("Ready_Process " + readyProcess.getId() + " waited for " + (currentTime - readyProcess.getTimeStamp()) + " units of time");// Get response time for process waiting here
                        System.out.println("Exit: " + readyProcess.getCurrActivity().code + "---" + readyProcess.getCurrEventCode());
                        readyActivity = readyProcess.getCurrActivity();
                        if (readyActivity.code == Process.ActivityCode.IO) { // Needs IO utilization
                            if (readyProcess.eventIsLast())
                                readyProcess.setCurrEventCode(Process.Event.EXIT);
                            else
                                readyProcess.setCurrEventCode(Process.Event.BLOCK);
                        } else { // Needs CPU utilization or Null current activity
                            if (readyActivity.code == Process.ActivityCode.CPU) {
                                System.out.println("Ready has waited since: " + readyProcess.getTimeStamp());
                                readyProcess.setWaitingTime(readyProcess.getTimeStamp());
                            }
                            readyProcess.setTimeStamp(currentTime);
                            readyProcess.setCurrEventCode(Process.Event.ARRIVE);
                        }
                        eventQueue.add(readyProcess); // Add to Event Queue to process
                    }
                    noExecution = true; // Set CPU is free
                    if (noProcessLeft()) // Check to terminate simulation
                        programTerminate = true;
                    break;
                case TIMEOUT:
                    System.out.println("TIMEOUT: Process " + currProcess.getId() + " overused CPU resources.");
            }
        }
        System.out.println("-------------END OF RUN LOG-------------");
        System.out.println("\nFinish HRRN in " + currentTime + " units of time.");
        printRes();
    }

    public void fbHandler() {
        System.out.println("Handling fb");
    }
}
