package org.yao.curator.x.discovery.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.yao.curator.x.discovery.server.Util;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class DiscoveryControllerTests {
  private TestRestTemplate template = new TestRestTemplate();

  private String basePath = "http://localhost:8000/v1/service";
  private String serviceName = "test";
  private String servicePath = basePath + "/" + serviceName;

  private String id1 = "001";
  private String id2 = "002";
  private String instance1Url = servicePath + "/" + id1;
  private String instance2Url = servicePath + "/" + id2;

  private String anyUrl = servicePath + "/any";

  @Test
  public void testBasics() throws Exception {
    JsonNode node;
    String res;

    // registerService
    ServiceInstance<String> instance1 =
        ServiceInstance.<String>builder()
            .payload("one")
            .name(serviceName)
            .port(10064)
            .id(id1)
            .serviceType(ServiceType.PERMANENT)
            .build();
    ServiceInstance<String> instance2 =
        ServiceInstance.<String>builder()
            .payload("two")
            .name(serviceName)
            .port(10064)
            .id(id2)
            .serviceType(ServiceType.PERMANENT)
            .build();
    res = template.postForObject(instance1Url, new HttpEntity<>(instance1), String.class);
    System.out.printf("response for registerService: %s\n", res);
    verifyNode(res);
    res = template.postForObject(instance2Url, new HttpEntity<>(instance2), String.class);
    System.out.printf("response for registerService: %s\n", res);
    verifyNode(res);

    // get
    res = template.getForObject(instance1Url, String.class);
    System.out.printf("response for get: %s\n", res);
    node = verifyNode(res);
    verifyIdAndName(node, serviceName, id1);

    // getAny
    res = template.getForObject(instance1Url, String.class);
    System.out.printf("response for getAny: %s\n", res);
    node = verifyNode(res);
    assertThat(node.get("data").get("name").textValue()).isEqualTo(serviceName);

    // getAllNames
    res = template.getForObject(basePath, String.class);
    System.out.printf("response for getAllNames: %s\n", res);
    node = verifyNode(res);
    List<String> actualServiceNames = new ArrayList<>();
    for (JsonNode child : node.get("data")) {
      actualServiceNames.add(child.textValue());
    }
    assertThat(actualServiceNames).containsExactly(serviceName);

    // getAll
    res = template.getForObject(servicePath, String.class);
    System.out.printf("response for getAll: %s\n", res);
    node = verifyNode(res);
    List<String> actualIds = new ArrayList<>();
    for (JsonNode child : node.get("data")) {
      actualIds.add(child.get("id").textValue());
    }
    assertThat(actualIds).containsExactly(id1, id2);

    // unregisterService
    ResponseEntity<String> entity =
        template.exchange(instance1Url, HttpMethod.DELETE, null, String.class);
    res = entity.getBody();
    System.out.printf("response for unregisterService with instance 1: %s\n", res);

    entity = template.exchange(instance2Url, HttpMethod.DELETE, null, String.class);
    res = entity.getBody();
    System.out.printf("response for unregisterService with instance 2: %s\n", res);
  }

  private JsonNode verifyNode(String s) {
    JsonNode node = Util.fromJson(s);
    assertThat(node.get("code").intValue()).isEqualTo(0);
    return node;
  }

  private void verifyIdAndName(JsonNode node, String name, String id) {
    JsonNode dataNode = node.get("data");
    assertThat(dataNode.get("id").textValue()).isEqualTo(id);
    assertThat(dataNode.get("name").textValue()).isEqualTo(name);
  }
}
