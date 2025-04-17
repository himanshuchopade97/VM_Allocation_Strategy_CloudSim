package cloudsim.simulation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.io.FileWriter;
import java.util.*;

//Enhanced version of your VMAllocationComparison class
public class VmAllocationComparison {
 public static void main(String[] args) {
     try {
         // Run Time-Shared VM scheduling simulation
         System.out.println("\n===== RUNNING TIME-SHARED VM SCHEDULING SIMULATION =====\n");
         long timeSharedStartTime = System.currentTimeMillis();
         List<Cloudlet> timeSharedResults = runSimulation("TimeShared", true);
         long timeSharedEndTime = System.currentTimeMillis();
         double timeSharedRuntime = (timeSharedEndTime - timeSharedStartTime) / 1000.0;
         
         // Run Space-Shared VM scheduling simulation
         System.out.println("\n===== RUNNING SPACE-SHARED VM SCHEDULING SIMULATION =====\n");
         long spaceSharedStartTime = System.currentTimeMillis();
         List<Cloudlet> spaceSharedResults = runSimulation("SpaceShared", false);
         long spaceSharedEndTime = System.currentTimeMillis();
         double spaceSharedRuntime = (spaceSharedEndTime - spaceSharedStartTime) / 1000.0;
         
         // Generate comparison report
         generateComparisonReport(timeSharedResults, spaceSharedResults, 
                                 timeSharedRuntime, spaceSharedRuntime);
         
         System.out.println("\nAll simulations completed. Reports generated.");
         
     } catch (Exception e) {
         e.printStackTrace();
     }
 }
 
 private static List<Cloudlet> runSimulation(String name, boolean isTimeShared) throws Exception {
     // Initialize CloudSim
     int numUsers = 1;
     Calendar calendar = Calendar.getInstance();
     CloudSim.init(numUsers, calendar, false);
     
     // Create Datacenter with appropriate scheduler
     Datacenter datacenter = createDatacenter(name + "_Datacenter", isTimeShared);
     
     // Create Broker
     DatacenterBroker broker = new DatacenterBroker(name + "_Broker");
     
     // Create VMs with appropriate scheduler
     List<Vm> vmlist = new ArrayList<>();
     for (int i = 0; i < 4; i++) {
         CloudletScheduler scheduler = isTimeShared ? 
             new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared();
             
         Vm vm = new Vm(i, broker.getId(), 1000, 1, 512, 1000, 1000,
                 "Xen", scheduler);
         vmlist.add(vm);
     }
     
     // Create Cloudlets
     List<Cloudlet> cloudletList = new ArrayList<>();
     for (int i = 0; i < 10; i++) {
         Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 30commit0, 300,
                 new UtilizationModelFull(), new UtilizationModelFull(), 
                 new UtilizationModelFull());
         cloudlet.setUserId(broker.getId());
         cloudletList.add(cloudlet);
     }
     
     broker.submitVmList(vmlist);
     broker.submitCloudletList(cloudletList);
     
     // Run Simulation
     CloudSim.startSimulation();
     List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
     CloudSim.stopSimulation();
     
     // Generate individual simulation log
     generateSimulationLog(name, finishedCloudlets, vmlist);
     
     return finishedCloudlets;
 }
 
 private static Datacenter createDatacenter(String name, boolean isTimeShared) throws Exception {
     List<Host> hostList = new ArrayList<>();
     
     for (int i = 0; i < 2; i++) {
         List<Pe> peList = new ArrayList<>();
         peList.add(new Pe(0, new PeProvisionerSimple(1000)));
         
         VmScheduler scheduler = isTimeShared ? 
             new VmSchedulerTimeShared(peList) : new VmSchedulerSpaceShared(peList);
             
         Host host = new Host(i, new RamProvisionerSimple(2048),
                 new BwProvisionerSimple(10000), 1000000, peList, scheduler);
                 
         hostList.add(host);
     }
     
     DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
             "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.1, 0.1);
             
     return new Datacenter(name, characteristics,
             new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
 }
 
 private static void generateSimulationLog(String strategy, List<Cloudlet> cloudlets, List<Vm> vms) {
     try {
         String logFileName = strategy + "_simulation_log.txt";
         FileWriter writer = new FileWriter(logFileName);
         
         writer.write("====== " + strategy + " VM SCHEDULING SIMULATION LOG ======\n\n");
         writer.write("SIMULATION CONFIGURATION:\n");
         writer.write("- Scheduling Strategy: " + strategy + "\n");
         writer.write("- Number of VMs: " + vms.size() + "\n");
         writer.write("- Number of Cloudlets: " + cloudlets.size() + "\n\n");
         
         writer.write("CLOUDLET EXECUTION RESULTS:\n");
         writer.write(String.format("%-10s %-10s %-15s %-15s %-15s %-15s\n", 
                      "Cloudlet", "Status", "VM ID", "Time", "Start Time", "Finish Time"));
         
         double totalExecutionTime = 0;
         int successfulCloudlets = 0;
         
         for (Cloudlet cloudlet : cloudlets) {
             boolean success = cloudlet.getStatus() == Cloudlet.SUCCESS;
             if (success) {
                 successfulCloudlets++;
                 totalExecutionTime += cloudlet.getActualCPUTime();
             }
             
             writer.write(String.format("%-10d %-10s %-15d %-15.2f %-15.2f %-15.2f\n",
                          cloudlet.getCloudletId(),
                          success ? "SUCCESS" : "FAILED",
                          cloudlet.getVmId(),
                          cloudlet.getActualCPUTime(),
                          cloudlet.getExecStartTime(),
                          cloudlet.getFinishTime()));
         }
         
         writer.write("\nPERFORMANCE SUMMARY:\n");
         writer.write("- Successful Cloudlets: " + successfulCloudlets + "/" + cloudlets.size() + "\n");
         if (successfulCloudlets > 0) {
             writer.write("- Average Execution Time: " + (totalExecutionTime / successfulCloudlets) + "\n");
             writer.write("- Makespan (total simulation time): " + getMaxFinishTime(cloudlets) + "\n");
         }
         
         writer.close();
         System.out.println("Simulation log generated: " + logFileName);
         
     } catch (Exception e) {
         e.printStackTrace();
     }
 }
 
 private static void generateComparisonReport(List<Cloudlet> timeSharedResults, 
                                           List<Cloudlet> spaceSharedResults,
                                           double timeSharedRuntime,
                                           double spaceSharedRuntime) {
     try {
         String reportFileName = "scheduling_comparison_report.txt";
         FileWriter writer = new FileWriter(reportFileName);
         
         writer.write("====== VM SCHEDULING STRATEGY COMPARISON REPORT ======\n\n");
         
         // Runtime comparison
         writer.write("RUNTIME COMPARISON:\n");
         writer.write("- Time-Shared Simulation Runtime: " + timeSharedRuntime + " seconds\n");
         writer.write("- Space-Shared Simulation Runtime: " + spaceSharedRuntime + " seconds\n");
         writer.write("- Runtime Difference: " + Math.abs(timeSharedRuntime - spaceSharedRuntime) + " seconds\n");
         writer.write("- Faster Strategy: " + (timeSharedRuntime < spaceSharedRuntime ? "Time-Shared" : "Space-Shared") + "\n\n");
         
         // Performance metrics
         double timeSharedMakespan = getMaxFinishTime(timeSharedResults);
         double spaceSharedMakespan = getMaxFinishTime(spaceSharedResults);
         
         double timeSharedAvgExecTime = getAverageExecutionTime(timeSharedResults);
         double spaceSharedAvgExecTime = getAverageExecutionTime(spaceSharedResults);
         
         writer.write("PERFORMANCE METRICS:\n");
         writer.write("- Time-Shared Makespan: " + timeSharedMakespan + "\n");
         writer.write("- Space-Shared Makespan: " + spaceSharedMakespan + "\n");
         writer.write("- Makespan Difference: " + Math.abs(timeSharedMakespan - spaceSharedMakespan) + "\n");
         writer.write("- Better Makespan: " + (timeSharedMakespan < spaceSharedMakespan ? "Time-Shared" : "Space-Shared") + "\n\n");
         
         writer.write("- Time-Shared Average Execution Time: " + timeSharedAvgExecTime + "\n");
         writer.write("- Space-Shared Average Execution Time: " + spaceSharedAvgExecTime + "\n");
         writer.write("- Average Execution Time Difference: " + Math.abs(timeSharedAvgExecTime - spaceSharedAvgExecTime) + "\n");
         writer.write("- Better Average Execution Time: " + 
                   (timeSharedAvgExecTime < spaceSharedAvgExecTime ? "Time-Shared" : "Space-Shared") + "\n\n");
         
         // Conclusion and recommendation
         writer.write("CONCLUSION:\n");
         writer.write("Based on the simulation results, the " + 
                   (timeSharedMakespan < spaceSharedMakespan ? "Time-Shared" : "Space-Shared") + 
                   " VM scheduling strategy performs better for overall completion time (makespan).\n\n");
         
         writer.write("The " + 
                   (timeSharedAvgExecTime < spaceSharedAvgExecTime ? "Time-Shared" : "Space-Shared") + 
                   " strategy performs better for average cloudlet execution time.\n\n");
         
         writer.write("RECOMMENDATION:\n");
         if (timeSharedMakespan < spaceSharedMakespan && timeSharedAvgExecTime < spaceSharedAvgExecTime) {
             writer.write("Use Time-Shared scheduling for both better makespan and better average execution time.\n");
         } else if (spaceSharedMakespan < timeSharedMakespan && spaceSharedAvgExecTime < timeSharedAvgExecTime) {
             writer.write("Use Space-Shared scheduling for both better makespan and better average execution time.\n");
         } else {
             writer.write("Choose scheduling strategy based on priority:\n");
             writer.write("- Use Time-Shared if average execution time is more important.\n");
             writer.write("- Use Space-Shared if overall completion time (makespan) is more important.\n");
         }
         
         writer.close();
         System.out.println("Comparison report generated: " + reportFileName);
         
     } catch (Exception e) {
         e.printStackTrace();
     }
 }
 
 private static double getMaxFinishTime(List<Cloudlet> cloudlets) {
     double maxFinishTime = 0;
     for (Cloudlet cloudlet : cloudlets) {
         if (cloudlet.getFinishTime() > maxFinishTime) {
             maxFinishTime = cloudlet.getFinishTime();
         }
     }
     return maxFinishTime;
 }
 
 private static double getAverageExecutionTime(List<Cloudlet> cloudlets) {
     double totalExecutionTime = 0;
     int successfulCloudlets = 0;
     
     for (Cloudlet cloudlet : cloudlets) {
         if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
             successfulCloudlets++;
             totalExecutionTime += cloudlet.getActualCPUTime();
         }
     }
     
     return successfulCloudlets > 0 ? totalExecutionTime / successfulCloudlets : 0;
 }
}