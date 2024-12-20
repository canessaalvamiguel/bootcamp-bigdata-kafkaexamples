package com.codigofacilito.customserde;

import com.codigofacilito.Constants;
import com.codigofacilito.Person;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.serialization.Serializer;

public final class PersonSerializer implements Serializer<Person> {

  @Override
  public byte[] serialize(final String topic, final Person data) {
    if (data == null) {
      return null;
    }
    try {
      return Constants.getJsonMapper().writeValueAsBytes(data);
    } catch (final JsonProcessingException e) {
      return null;
    }
  }

}
