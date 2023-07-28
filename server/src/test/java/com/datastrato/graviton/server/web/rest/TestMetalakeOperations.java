/*
 * Copyright 2023 Datastrato.
 * This software is licensed under the Apache License version 2.
 */
package com.datastrato.graviton.server.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.datastrato.graviton.MetalakeChange;
import com.datastrato.graviton.dto.MetalakeDTO;
import com.datastrato.graviton.dto.requests.MetalakeCreateRequest;
import com.datastrato.graviton.dto.requests.MetalakeUpdateRequest;
import com.datastrato.graviton.dto.requests.MetalakeUpdatesRequest;
import com.datastrato.graviton.dto.responses.DropResponse;
import com.datastrato.graviton.dto.responses.ErrorConstants;
import com.datastrato.graviton.dto.responses.ErrorResponse;
import com.datastrato.graviton.dto.responses.MetalakeListResponse;
import com.datastrato.graviton.dto.responses.MetalakeResponse;
import com.datastrato.graviton.exceptions.NoSuchMetalakeException;
import com.datastrato.graviton.meta.AuditInfo;
import com.datastrato.graviton.meta.BaseMetalake;
import com.datastrato.graviton.meta.BaseMetalakesOperations;
import com.datastrato.graviton.meta.SchemaVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMetalakeOperations extends JerseyTest {

  private static class MockServletRequestFactory extends ServletRequestFactoryBase {
    @Override
    public HttpServletRequest get() {
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getRemoteUser()).thenReturn(null);
      return request;
    }
  }

  private BaseMetalakesOperations metalakesOperations = mock(BaseMetalakesOperations.class);

  @Override
  protected Application configure() {
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.register(MetalakeOperations.class);
    resourceConfig.register(
        new AbstractBinder() {
          @Override
          protected void configure() {
            bind(metalakesOperations).to(BaseMetalakesOperations.class).ranked(2);
            bindFactory(MockServletRequestFactory.class).to(HttpServletRequest.class);
          }
        });

    return resourceConfig;
  }

  @Test
  public void testListMetalakes() {
    String metalakeName = "test";
    Long id = 1L;
    Instant now = Instant.now();
    AuditInfo info = new AuditInfo.Builder().withCreator("graviton").withCreateTime(now).build();
    BaseMetalake metalake =
        new BaseMetalake.Builder()
            .withName(metalakeName)
            .withId(id)
            .withAuditInfo(info)
            .withVersion(SchemaVersion.V_0_1)
            .build();

    when(metalakesOperations.listMetalakes()).thenReturn(new BaseMetalake[] {metalake, metalake});

    Response resp =
        target("/metalakes")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .get();

    Assertions.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

    MetalakeListResponse metalakeListResponse = resp.readEntity(MetalakeListResponse.class);
    Assertions.assertEquals(0, metalakeListResponse.getCode());

    MetalakeDTO[] metalakes = metalakeListResponse.getMetalakes();
    Assertions.assertEquals(2, metalakes.length);
    Assertions.assertEquals(metalakeName, metalakes[0].name());
    Assertions.assertEquals(metalakeName, metalakes[1].name());
  }

  @Test
  public void testCreateMetalake() {
    MetalakeCreateRequest req =
        new MetalakeCreateRequest("metalake", "comment", ImmutableMap.of("k1", "v1"));
    Instant now = Instant.now();

    BaseMetalake mockMetalake =
        new BaseMetalake.Builder()
            .withId(1L)
            .withName("metalake")
            .withComment("comment")
            .withProperties(ImmutableMap.of("k1", "v1"))
            .withAuditInfo(
                new AuditInfo.Builder().withCreator("graviton").withCreateTime(now).build())
            .withVersion(SchemaVersion.V_0_1)
            .build();

    when(metalakesOperations.createMetalake(any(), any(), any())).thenReturn(mockMetalake);

    Response resp =
        target("/metalakes")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

    Assertions.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

    MetalakeResponse metalakeResponse = resp.readEntity(MetalakeResponse.class);
    Assertions.assertEquals(0, metalakeResponse.getCode());

    MetalakeDTO metalake = metalakeResponse.getMetalake();
    Assertions.assertEquals("metalake", metalake.name());
    Assertions.assertEquals("comment", metalake.comment());
    Assertions.assertEquals(ImmutableMap.of("k1", "v1"), metalake.properties());

    MetalakeCreateRequest req1 = new MetalakeCreateRequest(null, null, null);
    Response resp1 =
        target("/metalakes")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .post(Entity.entity(req1, MediaType.APPLICATION_JSON_TYPE));
    Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp1.getStatus());

    ErrorResponse errorResponse = resp1.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.ILLEGAL_ARGUMENTS_CODE, errorResponse.getCode());
    Assertions.assertEquals(
        IllegalArgumentException.class.getSimpleName(), errorResponse.getType());
  }

  @Test
  public void testLoadMetalake() {
    String metalakeName = "test";
    Long id = 1L;
    Instant now = Instant.now();
    AuditInfo info = new AuditInfo.Builder().withCreator("graviton").withCreateTime(now).build();
    BaseMetalake metalake =
        new BaseMetalake.Builder()
            .withName(metalakeName)
            .withId(id)
            .withAuditInfo(info)
            .withVersion(SchemaVersion.V_0_1)
            .build();

    when(metalakesOperations.loadMetalake(any())).thenReturn(metalake);

    Response resp =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .get();

    Assertions.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    Assertions.assertEquals(MediaType.APPLICATION_JSON_TYPE, resp.getMediaType());

    MetalakeResponse metalakeResponse = resp.readEntity(MetalakeResponse.class);
    Assertions.assertEquals(0, metalakeResponse.getCode());

    MetalakeDTO metalake1 = metalakeResponse.getMetalake();
    Assertions.assertEquals(metalakeName, metalake1.name());
    Assertions.assertNull(metalake1.comment());
    Assertions.assertNull(metalake1.properties());

    // Test when specified metalake is not found.
    doThrow(new NoSuchMetalakeException("Failed to find metalake by name " + metalakeName))
        .when(metalakesOperations)
        .loadMetalake(any());

    Response resp1 =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .get();

    Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp1.getStatus());

    ErrorResponse errorResponse = resp1.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.NOT_FOUND_CODE, errorResponse.getCode());
    Assertions.assertEquals(NoSuchMetalakeException.class.getSimpleName(), errorResponse.getType());
    Assertions.assertEquals(
        "Metalake " + metalakeName + " does not exist", errorResponse.getMessage());

    // Test with internal error
    doThrow(new RuntimeException("Internal error")).when(metalakesOperations).loadMetalake(any());

    Response resp2 =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .get();

    Assertions.assertEquals(
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp2.getStatus());

    ErrorResponse errorResponse1 = resp2.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.INTERNAL_ERROR_CODE, errorResponse1.getCode());
    Assertions.assertEquals(RuntimeException.class.getSimpleName(), errorResponse1.getType());
    Assertions.assertEquals("Failed to load metalake " + metalakeName, errorResponse1.getMessage());
  }

  @Test
  public void testAlterMetalake() {
    String metalakeName = "test";
    Long id = 1L;
    Instant now = Instant.now();
    AuditInfo info = new AuditInfo.Builder().withCreator("graviton").withCreateTime(now).build();
    BaseMetalake metalake =
        new BaseMetalake.Builder()
            .withName(metalakeName)
            .withId(id)
            .withAuditInfo(info)
            .withVersion(SchemaVersion.V_0_1)
            .build();

    List<MetalakeUpdateRequest> updateRequests =
        Lists.newArrayList(
            new MetalakeUpdateRequest.RenameMetalakeRequest("newTest"),
            new MetalakeUpdateRequest.UpdateMetalakeCommentRequest("newComment"));
    MetalakeChange[] changes =
        updateRequests.stream()
            .map(MetalakeUpdateRequest::metalakeChange)
            .toArray(MetalakeChange[]::new);

    when(metalakesOperations.alterMetalake(any(), any(), any())).thenReturn(metalake);

    MetalakeUpdatesRequest req = new MetalakeUpdatesRequest(updateRequests);

    Response resp =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .put(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

    Assertions.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    MetalakeResponse metalakeResponse = resp.readEntity(MetalakeResponse.class);
    Assertions.assertEquals(0, metalakeResponse.getCode());

    MetalakeDTO metalake1 = metalakeResponse.getMetalake();
    Assertions.assertEquals(metalakeName, metalake1.name());
    Assertions.assertNull(metalake1.comment());
    Assertions.assertNull(metalake1.properties());

    // Test when specified metalake is not found.
    doThrow(new NoSuchMetalakeException("Failed to find metalake by name " + metalakeName))
        .when(metalakesOperations)
        .alterMetalake(any(), any(), any());

    Response resp1 =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .put(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

    Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp1.getStatus());
    ErrorResponse errorResponse = resp1.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.NOT_FOUND_CODE, errorResponse.getCode());
    Assertions.assertEquals(NoSuchMetalakeException.class.getSimpleName(), errorResponse.getType());

    // Test with internal error
    doThrow(new RuntimeException("Internal error"))
        .when(metalakesOperations)
        .alterMetalake(any(), any(), any());

    Response resp2 =
        target("/metalakes/" + metalakeName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .put(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

    Assertions.assertEquals(
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp2.getStatus());
    ErrorResponse errorResponse1 = resp2.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.INTERNAL_ERROR_CODE, errorResponse1.getCode());
    Assertions.assertEquals(RuntimeException.class.getSimpleName(), errorResponse1.getType());
  }

  @Test
  public void testDropMetalake() {
    when(metalakesOperations.dropMetalake(any())).thenReturn(true);
    Response resp =
        target("/metalakes/test")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .delete();

    Assertions.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    DropResponse dropResponse = resp.readEntity(DropResponse.class);
    Assertions.assertEquals(0, dropResponse.getCode());
    boolean dropped = dropResponse.dropped();
    Assertions.assertTrue(dropped);

    // Test throw an exception when deleting tenant.
    doThrow(new RuntimeException("Internal error")).when(metalakesOperations).dropMetalake(any());

    Response resp1 =
        target("/metalakes/test")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept("application/vnd.graviton.v1+json")
            .delete();

    Assertions.assertEquals(
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp1.getStatus());

    ErrorResponse errorResponse = resp1.readEntity(ErrorResponse.class);
    Assertions.assertEquals(ErrorConstants.INTERNAL_ERROR_CODE, errorResponse.getCode());
    Assertions.assertEquals(RuntimeException.class.getSimpleName(), errorResponse.getType());
    Assertions.assertEquals("Failed to drop metalake test", errorResponse.getMessage());
  }
}