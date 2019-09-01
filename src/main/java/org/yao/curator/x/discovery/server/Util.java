package org.yao.curator.x.discovery.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.x.discovery.ServiceInstance;

import java.io.IOException;

public class Util {
  private static ObjectMapper mapper = new ObjectMapper();

  public static String toJson(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode fromJson(String json) {
    try {
      return mapper.readTree(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
