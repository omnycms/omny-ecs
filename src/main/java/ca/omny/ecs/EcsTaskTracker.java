package ca.omny.ecs;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.NetworkBinding;
import com.amazonaws.services.ecs.model.Task;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcsTaskTracker {
    
    private AmazonECSClient ecsClient;
    private AmazonEC2Client ec2Client;
    
    Map<String, String> ec2InstanceIpMapping;
    Map<String,String> ec2IdToContainerArn;
    Set<String> knownContainers;
    
    Map<String,String> familyAndVersionToTaskDefinitionArnMap;
    public EcsTaskTracker() {
        ecsClient = new AmazonECSClient();
        ec2Client = new AmazonEC2Client();
        familyAndVersionToTaskDefinitionArnMap = new HashMap<>();
        ec2InstanceIpMapping = new HashMap<>();
        ec2IdToContainerArn = new HashMap<>();
        knownContainers = new HashSet<>();
    }
    
    public Map<String, List<Integer>> getHostPortMapping(String family, String version) {
        Map<String, List<Integer>> hostPortMapping = new HashMap<>();
        String cluster = System.getenv("OMNY_ECS_CLUSTER");

        String taskDefinitionArn = getTaskDefinitionArn(family, version);
        
        ListTasksResult listTasks = ecsClient.listTasks(new ListTasksRequest()
                .withCluster(cluster)
                .withFamily(family));
        DescribeTasksRequest r = new DescribeTasksRequest()
            .withCluster(cluster)
            .withTasks(listTasks.getTaskArns());
        DescribeTasksResult tasksResult = ecsClient.describeTasks(r);
        Map<String, List<Integer>> containerInstanceArnPortMapping = new HashMap<>();
        for(Task task: tasksResult.getTasks()) {
            if(task.getTaskDefinitionArn().equals(taskDefinitionArn)) {
                for(Container container: task.getContainers()) {
                    for(NetworkBinding binding: container.getNetworkBindings()) {
                        if(binding.getContainerPort()==8080||binding.getContainerPort()==80) {
                            int hostPort = binding.getHostPort();
                            String containerInstanceArn = task.getContainerInstanceArn();
                            if(!containerInstanceArnPortMapping.containsKey(containerInstanceArn)) {
                                containerInstanceArnPortMapping.put(containerInstanceArn,new LinkedList<Integer>());
                            }
                            List<Integer> ports = containerInstanceArnPortMapping.get(containerInstanceArn);
                            ports.add(hostPort);
                        }
                    }
                }
            }
        }
        Set<String> containerInstanceArns = containerInstanceArnPortMapping.keySet();
        List<String> unknownContainerArns = this.getUnknownContainerArns(containerInstanceArns);
        if(unknownContainerArns.size()>0) {
            DescribeContainerInstancesResult describeContainerInstances = ecsClient.describeContainerInstances(new DescribeContainerInstancesRequest().withCluster(cluster).withContainerInstances(unknownContainerArns));
            List<ContainerInstance> containerInstances = describeContainerInstances.getContainerInstances();

            for(ContainerInstance instance: containerInstances) {
                ec2IdToContainerArn.put(instance.getEc2InstanceId(), instance.getContainerInstanceArn());
                knownContainers.add(instance.getContainerInstanceArn());
            }
        }
        List<String> instanceIds = this.getUnknownInstances(ec2IdToContainerArn);
        if(instanceIds.size()>0) {
            DescribeInstancesResult describeInstances = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
            List<Reservation> reservations = describeInstances.getReservations();
            for(Reservation reservation: reservations) {
                for(Instance instance: reservation.getInstances()) {
                    String privateIpAddress = instance.getPrivateIpAddress();

                    //List<Integer> ports = containerInstanceArnPortMapping.get(ec2IdToContainerArn.get(instance.getInstanceId()));
                    //hostPortMapping.put(privateIpAddress, ports);
                    ec2InstanceIpMapping.put(instance.getInstanceId(), privateIpAddress);
                }
            }
        }
        
        addInstancesToMapping(hostPortMapping, ec2IdToContainerArn, containerInstanceArnPortMapping);
        return hostPortMapping;
    } 
    
    private List<String> getUnknownContainerArns(Collection<String> containerArns) {
        List<String> arns= new LinkedList<>();
        for(String arn: containerArns) {
            if(!knownContainers.contains(arn)) {
                arns.add(arn);
            }
        }
        return arns;
    }
    
    private List<String> getUnknownInstances(Map<String,String> ec2IdToContainerArn) {
        List<String> instanceIds= new LinkedList<>();
        for(String ec2Id: ec2IdToContainerArn.keySet()) {
            if(!ec2InstanceIpMapping.containsKey(ec2Id)) {
                instanceIds.add(ec2Id);
            }
        }
        return instanceIds;
    }
    
    private void addInstancesToMapping(Map<String, List<Integer>> hostPortMapping, Map<String,String> ec2IdToContainerArn, Map<String, List<Integer>> containerInstanceArnPortMapping) {
        for(String ec2Id: ec2IdToContainerArn.keySet()) {
            if(ec2InstanceIpMapping.containsKey(ec2Id)) {
                String privateIpAddress = ec2InstanceIpMapping.get(ec2Id);
                List<Integer> ports = containerInstanceArnPortMapping.get(ec2IdToContainerArn.get(ec2Id));
                hostPortMapping.put(privateIpAddress, ports);
            }
        }
    }

    private String getTaskDefinitionArn(String family, String version) {
        String familyAndVersionKey = family+":"+version;
        if(!familyAndVersionToTaskDefinitionArnMap.containsKey(family+":"+version)) {
            DescribeTaskDefinitionResult describeTaskDefinition = ecsClient.describeTaskDefinition(new DescribeTaskDefinitionRequest().withTaskDefinition(familyAndVersionKey));
            familyAndVersionToTaskDefinitionArnMap.put(familyAndVersionKey, describeTaskDefinition.getTaskDefinition().getTaskDefinitionArn());
        }
        String taskDefinitionArn = familyAndVersionToTaskDefinitionArnMap.get(familyAndVersionKey);
        return taskDefinitionArn;
    }
}
