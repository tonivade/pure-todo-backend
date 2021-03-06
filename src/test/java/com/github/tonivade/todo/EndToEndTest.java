/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.zeromock.api.Bytes.asBytes;
import static com.github.tonivade.zeromock.api.Bytes.asString;
import static com.github.tonivade.zeromock.api.Requests.delete;
import static com.github.tonivade.zeromock.api.Requests.get;
import static com.github.tonivade.zeromock.api.Requests.post;
import static com.github.tonivade.zeromock.api.Requests.patch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tonivade.purejson.PureJson;
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.application.TodoDTO;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.client.UIOHttpClient;
import com.github.tonivade.zeromock.junit5.MockHttpServerExtension;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;

@ExtendWith(MockHttpServerExtension.class)
class EndToEndTest {
  
  private static final String TODO = "/todo";

  private Config config = Application.loadConfig();
  
  private HttpUIOService service = Application.buildService(config);
  
  @Test
  void emptyArrayWhenEmpty(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);
    
    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());
    assertEquals(asBytes("[]"), response.body());
  }
  
  @Test
  void createItem(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.CREATED, response.status());

    TodoDTO dto = parseItem(response);
    assertNotNull(dto.url());
    assertEquals("asdfg", dto.title());
  }
  
  @Test
  void afterCreateItemThenReturned(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(1, list.size());
    assertEquals("asdfg", list.get(0).title());
  }
  
  @Test
  void createTwoItems(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .andThen(client.request(createNew("qwert")))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(2, list.size());
    assertEquals("asdfg", list.get(0).title());
    assertEquals("qwert", list.get(1).title());
  }
  
  @Test
  void updateTitle(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg"))).map(this::parseItem)
        .flatMap(item -> client.request(updateTitle(item.id(), "qwert")))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(1, list.size());
    assertEquals("qwert", list.get(0).title());
  }
  
  @Test
  void updateOrder(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg"))).map(this::parseItem)
        .flatMap(item -> client.request(updateOrder(item.id(), 3)))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(1, list.size());
    assertEquals(3, list.get(0).order());
  }
  
  @Test
  void updateCompleted(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg"))).map(this::parseItem)
        .flatMap(item -> client.request(updateCompleted(item.id(), true)))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(1, list.size());
    assertEquals(true, list.get(0).completed());
  }
  
  @Test
  void updateTitleOrderAndCompleted(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount(TODO, service);

    HttpResponse response = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg"))).map(this::parseItem)
        .flatMap(item -> client.request(updateTitleOrderAndCompleted(item.id(), "qwert", 3, true)))
        .andThen(client.request(getAll()))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());

    List<TodoDTO> list = parseList(response);
    assertEquals(1, list.size());
    TodoDTO item = list.get(0);
    assertEquals("qwert", item.title());
    assertEquals(3, item.order());
    assertEquals(true, item.completed());
  }

  private List<TodoDTO> parseList(HttpResponse response) {
    Type listOfTodos = new TypeToken<List<TodoDTO>>() {}.getType();
    return new PureJson<List<TodoDTO>>(listOfTodos).fromJson(asString(response.body())).get().get();
  }

  private TodoDTO parseItem(HttpResponse response) {
    return new PureJson<>(TodoDTO.class).fromJson(asString(response.body())).get().get();
  }

  private HttpRequest deleteAll() {
    return delete(TODO);
  }

  private HttpRequest getAll() {
    return get(TODO);
  }

  private HttpRequest createNew(String title) {
    return post(TODO)
      .withHeader("Content-type", "application/json")
      .withBody(
          """
          {"title":"%s"}
          """.formatted(title));
  }

  private HttpRequest updateTitleOrderAndCompleted(int id, String title, int order, boolean completed) {
    return patch(TODO + "/" + id)
      .withHeader("Content-type", "application/json")
      .withBody(
          """
          { 
              "title": "%s",
              "order": %s,
              "completed": %s
          }
          """.formatted(title, order, completed));
  }

  private HttpRequest updateTitle(int id, String title) {
    return patch(TODO + "/" + id)
      .withHeader("Content-type", "application/json")
      .withBody(
          """
          {"title": "%s"}
          """.formatted(title));
  }

  private HttpRequest updateOrder(int id, int order) {
    return patch(TODO + "/" + id)
      .withHeader("Content-type", "application/json")
      .withBody(
          """
          {"order": %s}
          """.formatted(order));
  }

  private HttpRequest updateCompleted(int id, boolean completed) {
    return patch(TODO + "/" + id)
      .withHeader("Content-type", "application/json")
      .withBody(
          """
          {"completed": %s}
          """.formatted(completed));
  }
}
