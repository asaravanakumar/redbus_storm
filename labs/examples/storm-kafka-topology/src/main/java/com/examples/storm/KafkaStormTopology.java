package com.examples.storm;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.kafka.bolt.selector.DefaultTopicSelector;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

import java.util.Properties;
import java.util.UUID;

public class KafkaStormTopology {
   public static void main(String[] args) throws Exception{

      // Kafka server and topic details
      final String KAFKA_SERVER_HOST = "localhost:9092";
      final String KAFKA_INPUT_TOPIC_NAME = "storm-wc-input";
      final String KAFKA_OUTPUT_TOPIC_NAME = "storm-wc-output";
      final String KAFKA_CONSUMER_GROUP_NAME = "storm-wc-group";

      // Create topology
      TopologyBuilder builder = new TopologyBuilder();

      // Add Spout - to randomly generate the sentences
//      builder.setSpout("sentences", new RandomSentenceSpout(), 1);

      // Add Spout - to read the message from Kafka topic
      builder.setSpout("sentences", new KafkaSpout<>(KafkaSpoutConfig.builder(KAFKA_SERVER_HOST, KAFKA_INPUT_TOPIC_NAME).setProp("group.id",KAFKA_CONSUMER_GROUP_NAME).build()), 1);

      // Add Bolt - to split the sentences into words
      builder.setBolt("splitter",  new SplitSentenceBolt(), 1)
              .shuffleGrouping("sentences");

      // Add Bolt - to count the words
      builder.setBolt("counter", new WordCountBolt(), 1)
              .fieldsGrouping("splitter", new Fields("word"));

      // Add Bolt - to format the message to publish to Kafka topic
      builder.setBolt("formatter", new FormatterBolt(), 1)
              .shuffleGrouping("counter");

      // Add Bolt - to print the tuple details
      builder.setBolt("printer", new PrinterBolt(), 1)
              .shuffleGrouping("formatter");

      // Add Bolt - to write the message into Kafka topic
      // set producer properties.
      Properties props = new Properties();
      props.put("bootstrap.servers", KAFKA_SERVER_HOST);
      props.put("acks", "1");
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

      // Create Kafka Bolt
      KafkaBolt bolt = new KafkaBolt()
              .withProducerProperties(props)
              .withTopicSelector(new DefaultTopicSelector(KAFKA_OUTPUT_TOPIC_NAME))
              .withTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper());

      // Publish message to Kafka topic
      builder.setBolt("forwardToKafka", bolt, 1).shuffleGrouping("formatter");

      // Configure the topology
      Config conf = new Config();
      conf.setDebug(true);
      conf.setNumWorkers(3);

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
//               Utils.sleep(60000);
//               // Bring down the local cluster
//               cluster.shutdown();
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