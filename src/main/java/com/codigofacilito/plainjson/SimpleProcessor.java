package com.codigofacilito.plainjson;

import com.codigofacilito.Constants;
import com.codigofacilito.Person;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class SimpleProcessor {
  private final Logger log = LoggerFactory.getLogger(SimpleProcessor.class);

  private final Consumer<String, String> consumer;
  private final Producer<String, String> producer;

  public SimpleProcessor(final String brokers) {
    final Properties consumerProps = new Properties();
    consumerProps.put("bootstrap.servers", brokers);
    consumerProps.put("group.id", "person-processor");
    consumerProps.put("key.deserializer", StringDeserializer.class);
    consumerProps.put("value.deserializer", StringDeserializer.class);
    consumer = new KafkaConsumer<>(consumerProps);

    final Properties producerProps = new Properties();
    producerProps.put("bootstrap.servers", brokers);
    producerProps.put("key.serializer", StringSerializer.class);
    producerProps.put("value.serializer", StringSerializer.class);
    producer = new KafkaProducer<>(producerProps);
  }

  public static void main(final String[] args) {
    new SimpleProcessor("localhost:29092").process();
  }

  @SuppressWarnings("InfiniteLoopStatement")
  public void process() {
    consumer.subscribe(Collections.singletonList(Constants.getPersonsTopic()));

    while (true) {
      final ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1L));

      for (final ConsumerRecord<String, String> record : records) {
        final String personJson = record.value();
        log.debug("JSON data: {}", personJson);
        Person person = null;
        try {
          person = Constants.getJsonMapper().readValue(personJson, Person.class);
        } catch (final IOException e) {
          log.error(e.getMessage());
        }
        log.debug("Person: {}", person);
        assert person != null;
        final LocalDate startDate = person.birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        final int age = Period.between(startDate, LocalDate.now()).getYears();
        log.debug("Age: {}", age);
        final Future<RecordMetadata> future = producer.send(new ProducerRecord<>(
            Constants.getAgesTopic(), person.firstName + " " + person.lastName, String.valueOf(age)));
        try {
          future.get();
        } catch (final InterruptedException | ExecutionException e) {
          log.error(e.getMessage());
        }
      }
    }
  }
}
