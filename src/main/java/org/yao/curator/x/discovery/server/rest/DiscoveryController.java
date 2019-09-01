package org.yao.curator.x.discovery.server.rest;

import com.google.common.collect.Lists;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ThreadUtils;
import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceProvider;
import org.apache.curator.x.discovery.strategies.RandomStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * Implements all the HTTP API on https://curator.apache.org/curator-x-discovery-server/index.html.
 * The API does not follow the REST way. HTTP status codes are not used to carry API method return
 * status. A code in Response object is used for this purpose.
 */
@RestController
@RequestMapping("/v1/service")
public class DiscoveryController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private ServiceDiscovery<String> serviceDiscovery;
  private CuratorFramework client;
  private ProviderStrategy<String> providerStrategy;

  @Value("${connectString}")
  private String connectString;

  @Value("${basePath}")
  private String basePath;

  @PostConstruct
  public void start() throws Exception {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
    client.start();

    serviceDiscovery =
        ServiceDiscoveryBuilder.builder(String.class).basePath(basePath).client(client).build();
    serviceDiscovery.start();

    providerStrategy = new RandomStrategy<>();
  }

  @PreDestroy
  public void close() throws Exception {
    try {
      serviceDiscovery.close();
    } catch (Throwable ex) {
      log.error("error when closing service discovery", ex);
      throw ex;
    }
    try {
      client.close();
    } catch (Throwable ex) {
      log.error("error when closing curator framework", ex);
      throw ex;
    }
  }

  @GetMapping(path = "{name}/{id}")
  public Response get(@PathVariable String name, @PathVariable String id) throws Exception {
    Response res = new Response();
    ServiceInstance<String> instance = serviceDiscovery.queryForInstance(name, id);
    res.setData(instance);
    return res;
  }

  @GetMapping(path = "{name}/any")
  public Response getAny(@PathVariable String name) throws Exception {
    Response res = new Response();
    final List<ServiceInstance<String>> instances =
        Lists.newArrayList(serviceDiscovery.queryForInstances(name));
    ServiceInstance<?> randomInstance =
        providerStrategy.getInstance(
            new InstanceProvider<String>() {
              @Override
              public List<ServiceInstance<String>> getInstances() throws Exception {
                return instances;
              }
            });
    if (randomInstance == null) {
      res.setCode(100);
      res.setMsg(String.format("no instances for %s service", name));
      return res;
    }
    res.setData(randomInstance);
    return res;
  }

  @GetMapping(path = "{name}")
  public Response getAll(@PathVariable String name) throws Exception {
    Response res = new Response();
    res.setData(serviceDiscovery.queryForInstances(name));
    return res;
  }

  @GetMapping(path = "")
  public Response getAllNames() throws Exception {
    Response res = new Response();
    res.setData(serviceDiscovery.queryForNames());
    return res;
  }

  @GetMapping(path = "about")
  public String about() throws Exception {
    return "curator-x-discovery-server";
  }

  @DeleteMapping(path = "{name}/{id}")
  public Response unregisterService(@PathVariable String name, @PathVariable String id)
      throws Exception {
    Response res = new Response();
    ServiceInstance instance = serviceDiscovery.queryForInstance(name, id);
    if (instance != null) {
      serviceDiscovery.unregisterService(instance);
    }
    return res;
  }

  @PostMapping(path = "{name}/{id}")
  public Response registerService(
      @RequestBody ServiceInstance<String> instance,
      @PathVariable("name") String name,
      @PathVariable("id") String id)
      throws Exception {
    Response res = new Response();
    if (!instance.getId().equals(id)) {
      String msg =
          String.format("path id '{}' and instance id '{}' does not match", id, instance.getId());
      log.info(msg);
      res.setCode(100);
      res.setMsg(msg);
      return res;
    }
    if (!instance.getName().equals(name)) {
      String msg =
          String.format(
              "path name '{}' and instance name '{}' does not match", name, instance.getName());
      log.info(msg);
      res.setCode(200);
      res.setMsg(msg);
      return res;
    }
    if (instance.getServiceType().isDynamic()) {
      String msg = "Service type cannot be dynamic";
      log.info(msg);
      res.setCode(200);
      res.setMsg(msg);
      return res;
    }
    serviceDiscovery.registerService(instance);
    return res;
  }
}
