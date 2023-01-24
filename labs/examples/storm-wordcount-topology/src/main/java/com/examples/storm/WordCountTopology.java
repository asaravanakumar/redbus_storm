package com.examples.storm;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

public class WordCountTopology {
    public static void main(String[] args) {

        // Create topology
        TopologyBuilder builder = new TopologyBuilder();

        // Add Spouts - to generate random sentence
        builder.setSpout("sentences",  new RandomSentenceSpout(), 1);

        // Add Bolts - to split the sentences into words
        builder.setBolt("splitter",  new SplitSentenceBolt(), 1)
                .shuffleGrouping("sentences");

        // Add Bolts - to count the words
        builder.setBolt("counter", new WordCountBolt(), 1)
                .fieldsGrouping("splitter", new Fields("word"));

        // Configure the topology
        Config conf = new Config();
        conf.setDebug(true);
        conf.setNumWorkers(1);

        // Default topology name
        String topologyName = "wc-topology";
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
                } catch (AlreadyAliveException | InvalidTopologyException | AuthorizationException e) {
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
