package com.examples.storm;

import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.topology.TopologyBuilder;

/**
 * Main program to create topology and submits to distributed cluster.
 */
public class ExclamationTopology extends ConfigurableTopology {

    public static void main(String[] args) {
        // Creates topology as separate thread
        ConfigurableTopology.start(new ExclamationTopology(), args);
    }

    @Override
    protected int run(String[] args) throws Exception {
        // Create topology
        TopologyBuilder builder = new TopologyBuilder();
        // Add Spouts and Bolts
        builder.setSpout("words", new RandomWordsSpout(), 1);
        builder.setBolt("exclaim1", new ExclamationBolt(), 1)
                .shuffleGrouping("words");

        // Topology configuration
        conf.setDebug(true);
        conf.setNumWorkers(1);

        // Default topology name
        String topologyName = "exc-topology";

        // Check and read the topology name if passed as arguments
        if (args != null && args.length > 0) {
            topologyName = args[0];
        }

        // Submits topology to distributed cluster
        return submit(topologyName, conf, builder);
    }
}
