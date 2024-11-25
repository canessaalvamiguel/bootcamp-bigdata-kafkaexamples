package com.codigofacilito.plainjson;

import com.codigofacilito.Constants;
import com.codigofacilito.Person;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class SimpleProducer {
  private final Logger log = LoggerFactory.getLogger(SimpleProducer.class);
  private final Producer<String, String> producer;

  public SimpleProducer(final String brokers) {
    final Properties props = new Properties();
    props.put("bootstrap.servers", brokers);
    props.put("key.serializer", StringSerializer.class);
    props.put("value.serializer", StringSerializer.class);
    producer = new KafkaProducer<>(props);
  }

  public static void main(final String[] args) {
    new SimpleProducer("localhost:29092").produce(2);
  }

  @SuppressWarnings("InfiniteLoopStatement")
  public void produce(final int ratePerSecond) {
    final long waitTimeBetweenIterationsMs = 1000L / (long) ratePerSecond;
    final Faker faker = new Faker();

    while (true) {
      final Person fakePerson = new Person(
          faker.name().firstName(),
          faker.name().lastName(),
          faker.date().birthday(),
          faker.address().city(),
          faker.internet().ipV4Address()
      );
      log.debug("Produced Person: {}", fakePerson);
      String fakePersonJson = null;
      try {
        fakePersonJson = Constants.getJsonMapper().writeValueAsString(fakePerson);
      } catch (final JsonProcessingException e) {
        log.error(e.getMessage());
      }
      final Future<RecordMetadata> futureResult =
          producer.send(new ProducerRecord<>(Constants.getPersonsTopic(), fakePersonJson));
      try {
        Thread.sleep(waitTimeBetweenIterationsMs);
        futureResult.get();
      } catch (final InterruptedException | ExecutionException e) {
        log.error(e.getMessage());
      }
    }
  }
}
