package com.examples.storm;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;

/**
 * Main program to create topology and submits to local or distributed cluster.
 */
public class ExclamationLocalTopology {

    public static void main(String[] args) {
        // Create topology
        TopologyBuilder builder = new TopologyBuilder();

        // Add Spouts and Bolts
        builder.setSpout("words", new RandomWordsSpout(), 1);
        builder.setBolt("exclaim1", new ExclamationBolt(), 1)
                .shuffleGrouping("words");
        builder.setBolt("exclaim2", new ExclamationBolt(), 1)
                .shuffleGrouping("exclaim1");

        // Configure the topology
        Config conf = new Config();
        conf.setDebug(true);
        conf.setNumWorkers(1);

        // Default topology name
        String topologyName = "exc-topology";
        // Default mode to execute topology
        String mode = "local";

        // Check and Read the topology name if passed as arguments
        if (args != null && args.length == 1) {
            topologyName = args[0];
        }

        // Check and Read the topology name and mode if passed as arguments
        if (args != null && args.length == 2) {
            topologyName = args[0];
            mode = args[1];
        }

        switch (mode) {
            case "local":
                try {
                    LocalCluster cluster =new LocalCluster();
                    // Submit topology to local cluster
                    cluster.submitTopology(topologyName, conf, builder.createTopology());
                    // Wait for 10 secs to complete topology execution
                    Utils.sleep(10000);
                    // Bring down the local cluster
                    cluster.shutdown();
                } catch (Exception e) {
                    System.out.println("Error submitting topology. " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case "dist":
                try {
                    // Submit topology to distributed cluster
                    StormSubmitter.submitTopology(topologyName, conf, builder.createTopology());
                } catch (AlreadyAliveException | InvalidTopologyException |AuthorizationException e) {
                    System.out.println("Error submitting topology. " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Invalid mode - " + mode + ". Expected [dist | local]");
                System.exit(0);
        }

    }
}
