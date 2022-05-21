import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;

public class Process implements Comparable<Process> {
    private final int id; // Process id
    private int timeStamp; // Timestamp of the process
    private final int arrivalTime; // The time the process is loaded
    private int serviceTime; // sum of the durations for all CPU events for that process
    private int startTime; // The time the processes first gains control of the cpu
    private int finishTime; // The time when the last event of the process is complete
    private int turnaroundTime; // The duration between the arrival time and finish time
    private int waitingTimestamp; // Time taken the process to wait for CPU activities
    private int initCPUDuration; // Duration of the first CPU
    private int remainingBurstTime; // Total remaining CPU occupying time
    private float normalizedTurnaroundTime; // (turnaround/service)
    private float averageResponseTime; // Average waiting times divided number of wait-for-CPU activity
    private float responseRatio; // Response ration between waiting time and service time
    private final String scheduleAlgorithm; // Name of currently running scheduling algorithm
    private Activity currActivity; // Current activity with code and duration of the process
    private Event currEventCode; // Current event code of the process
    Deque<Activity> activityQueue = new LinkedList<>(); // Holds activities of the process
    int waitForCpuCount = 0; // Number of times the process has to wait for CPU access

    @Override
    public int compareTo(Process o) { // Helper function for Priority queue
        if (scheduleAlgorithm.matches("FCFS|VRR"))
            return Comparator.comparing(Process::getTimeStamp).thenComparing(Process::getEventComp).thenComparing(Process::getId).compare(this, o);
        else if (scheduleAlgorithm.matches("SRT"))
            return Comparator.comparing(Process::getTimeStamp).thenComparing(Process::getEventComp).thenComparing(Process::getRemainingBurstTime).thenComparing(Process::getId).compare(this, o);
        else
            return Comparator.comparing(Process::getTimeStamp).thenComparing(Process::getEventComp).thenComparing(Comparator.comparing(Process::getResponseRatio).reversed()).thenComparing(Process::getId).compare(this, o);
    }

    static class Activity { // Class of Activity
        ActivityCode code;
        int duration;
    }

    enum ActivityCode {
        CPU, // needs to run on the cpu
        IO //  blocked for the duration
    }

    enum Event {
        ARRIVE, // The Process is launched
        BLOCK, // The Process is currently running on the cpu and will block when this event happens
        EXIT, // The Process is currently running on the cpu and will terminate when this event happens
        UNBLOCK, // The Process is currently in the block state, waiting on IO and will unblock when this event happens
        TIMEOUT // The Process is currently running on the cpu and will timeout when this event happens
    }

    public Process(int id, int arrivalTime, String scheduleAlgorithm, String activities) {
        this.id = id;
        this.scheduleAlgorithm = scheduleAlgorithm;
        this.timeStamp = arrivalTime;
        this.arrivalTime = arrivalTime;
        this.serviceTime = 0;
        this.startTime = -1;
        this.finishTime = -1;
        this.turnaroundTime = 0;
        this.averageResponseTime = 0;
        this.waitingTimestamp = -1;
        this.remainingBurstTime = 0;
        this.responseRatio = 0;
        this.currActivity = new Activity();
        ActListToQueues(activities.split(" ", 0));
    }

    public void ActListToQueues(String[] activityList) {
        Activity input = new Activity();
        for (int i = 0; i < activityList.length - 1; i += 2) {
            input.code = ActivityCode.valueOf(activityList[i]);
            input.duration = Integer.parseInt(activityList[i + 1]);
            activityQueue.addLast(input);
            input = new Activity();
        }
    }

    public int getId() {
        return id;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void addServiceTime(int serviceTime) {
        this.serviceTime += serviceTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime() {
        this.turnaroundTime = this.finishTime - this.arrivalTime;
    }

    public float getNormalizedTurnaroundTime() {
        return this.normalizedTurnaroundTime;
    }

    public void setNormalizedTurnaroundTime() {
        if (this.serviceTime == 0) {
            System.err.println("\nnormalizedTurnaroundTime: Process " + this.id + "'s \"serviceTime\" must be non-zero!\n");
        } else {
            this.normalizedTurnaroundTime = ((float) this.turnaroundTime / this.serviceTime);
        }
    }

    public float getAverageResponseTime() {
        return this.averageResponseTime;
    }

    public void setAverageResponseTime() {
        if (waitForCpuCount != 0)
            this.averageResponseTime /= waitForCpuCount;
        else
            this.averageResponseTime = 0;
    }

    public void addAverageResponseTime(double averageResponseTime) {
        if (averageResponseTime != 0) {
            this.averageResponseTime += averageResponseTime;
            waitForCpuCount++;
        }
    }

    public int getWaitingTimestamp() {
        return waitingTimestamp;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTimestamp = waitingTime;
    }

    public int getInitCPUDuration() {
        return initCPUDuration;
    }

    public void setInitCPUDuration(int initCPUDuration) {
        this.initCPUDuration = initCPUDuration;
    }

    public int getRemainingBurstTime() {
        return remainingBurstTime;
    }

    public void setRemainingBurstTime(int remainingBurstTime) {
        this.remainingBurstTime = remainingBurstTime;
    }

    public void addRemainingBurstTime(int CpuTime) { this.remainingBurstTime += CpuTime; }

    public void removeRemainingBurstTime(int CpuTime) { this.remainingBurstTime -= CpuTime; }

    public float getResponseRatio() { return responseRatio; }

    public void calculateResponseRatio(int wait, int serviceTime) {
        if (wait >= 0)
            this.responseRatio = ((float) wait / serviceTime + 1);
    }

    public void getNextCurrActivity() {
        if (!(activityQueue.isEmpty())) {
            this.currActivity = activityQueue.removeFirst();
        } else
            System.out.println("Warning: unable to update CurrentActivity." +
                    "\nCause: empty Activity Queues.");
    }

    public Activity peakActivityQueue() {
        return activityQueue.peek();
    }

    public void updateActivityQueueHead(Activity activity) {
        activityQueue.addFirst(activity);
    }

    public Activity getCurrActivity() {
        return currActivity;
    }

    public Event getCurrEventCode() {
        return currEventCode;
    }

    public void setCurrEventCode(Event currEventCode) {
        this.currEventCode = currEventCode;
    }

    public boolean eventIsLast() {
        return activityQueue.isEmpty();
    }

    public int getEventComp() {
        if (currEventCode == Event.TIMEOUT)
            return 0;
        else if (currEventCode == Event.BLOCK)
            return 1;
        else
            return 2;
    }

    @Override
    public String toString() {
        return "Process " + id + ": {" +
                "arrivalTime=" + arrivalTime +
                ", startTime=" + startTime +
                ", finishTime=" + finishTime +
                ", serviceTime=" + serviceTime +
                ", turnaroundTime=" + turnaroundTime +
                ", normalizedTurnaroundTime=" + normalizedTurnaroundTime +
                ", averageResponseTime=" + averageResponseTime +
                '}';
    }
}