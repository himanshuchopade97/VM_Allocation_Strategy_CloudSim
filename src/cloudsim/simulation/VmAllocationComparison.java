package cloudsim.simulation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

public class VmAllocationComparison {

    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            // Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_1");

            // Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker");

            // Create VMs
            List<Vm> vmlist = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Vm vm = new Vm(i, broker.getId(), 1000, 1, 512, 1000, 1000,
                        "Xen", new CloudletSchedulerSpaceShared()); // or CloudletSchedulerSpaceShared()
                vmlist.add(vm);
            }

            // Create Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 300, 300,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                cloudlet.setUserId(broker.getId());
                cloudletList.add(cloudlet);
            }

            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            // Run Simulation
            CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printCloudletList(newList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create simple datacenter with given scheduling
    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));

            Host host = new Host(i, new RamProvisionerSimple(2048),
                    new BwProvisionerSimple(10000), 1000000, peList,
                    new VmSchedulerSpaceShared(peList)); // Try VmSchedulerSpaceShared for FCFS

            hostList.add(host);
        }

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.1, 0.1);

        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "\t";
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent +
                "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.println(cloudlet.getCloudletId() + indent + "SUCCESS" + indent +
                        cloudlet.getActualCPUTime() + indent +
                        cloudlet.getExecStartTime() + indent + cloudlet.getFinishTime());
            }
        }
    }
}