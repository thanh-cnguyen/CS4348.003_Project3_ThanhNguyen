import java.util.Arrays;
import java.util.Scanner;

public class Driver {
    public static void main(String[] args) {
        try {
            // Get files from the terminal
            String[] scheFile = args[0].split("\\.", 2);
            String[] proFile = args[1].split("\\.", 2);

            // Sample inputs
//            String[] scheFile = new String[2];
//            String[] proFile = new String[2];
//            scheFile[0] = "vrr";
//            scheFile[1] = "sf";
//            proFile[0] = "prof";
//            proFile[1] = "pf";

            if (scheFile[1].equalsIgnoreCase("sf")) {
                ScheduleAlgorithm sa = new ScheduleAlgorithm(scheFile);
                if (proFile[1].equalsIgnoreCase("pf")) {
                    sa.getProcesses(proFile);
                    switch (scheFile[0]) {
                        case "fcfs":
                            sa.fcfsHandler();
                            break;
                        case "vrr":
                            sa.vrrHandler();
                            break;
                        case "srt":
                            sa.srtHandler();
                            break;
                        case "hrrn":
                            sa.hrrnHandler();
                            break;
                        case "fb":
                            sa.fbHandler();
                            break;
                        default:
                            System.err.println("Invalid Schedule Algorithm file name");
                    }
                } else
                    System.err.println("Invalid process file extension");
            } else
                System.err.println("Invalid schedule file extension");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
