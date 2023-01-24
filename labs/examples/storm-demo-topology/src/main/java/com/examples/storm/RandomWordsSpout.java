package com.examples.storm;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.testing.TestWordSpout;
import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomWordsSpout implements IRichSpout {
    public static Logger LOG = LoggerFactory.getLogger(TestWordSpout.class);
    boolean isDistributed;
    SpoutOutputCollector collector;

    public RandomWordsSpout() {
        this(true);
    }

    public RandomWordsSpout(boolean isDistributed) {
        this.isDistributed = isDistributed;
    }

    public void open(Map<String, Object> conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
    }

    public void close() {
    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }

    public void nextTuple() {
        Utils.sleep(100L);
        String[] words = new String[]{"nathan", "mike", "jackson", "golda", "bertels"};
        Random rand = new Random();
        String word = words[rand.nextInt(words.length)];
        this.collector.emit(new Values(new Object[]{word}));
    }

    public void ack(Object msgId) {
    }

    public void fail(Object msgId) {
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(new String[]{"word"}));
    }

    public Map<String, Object> getComponentConfiguration() {
        if (!this.isDistributed) {
            Map<String, Object> ret = new HashMap();
            ret.put("topology.max.task.parallelism", 1);
            return ret;
        } else {
            return null;
        }
    }
}
