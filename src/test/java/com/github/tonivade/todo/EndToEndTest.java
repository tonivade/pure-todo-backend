/*
 * Copyright (c) 2020-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.zeromock.api.Requests.delete;
import static com.github.tonivade.zeromock.api.Requests.get;
import static com.github.tonivade.zeromock.api.Requests.patch;
import static com.github.tonivade.zeromock.api.Requests.post;
import static com.github.tonivade.zeromock.client.UIOHttpClient.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.lang.reflect.Type;
import java.net.HttpRetryException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.application.TodoDTO;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.client.UIOHttpClient;
import com.github.tonivade.zeromock.junit5.MockHttpServerExtension;
import com.github.tonivade.zeromock.junit5.Mount;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;

@ExtendWith(MockHttpServerExtension.class)
class EndToEndTest {
  
  private static final String TODO = "/todo";

  final Config config = Application.loadConfig();

  final Type listOfTodos = new TypeToken<ImmutableList<TodoDTO>>() {}.getType();
  
  @Mount(TODO)
  final HttpUIOService service = Application.buildService(config);
  
  @Test
  void emptyArrayWhenEmpty(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).isEmpty();
  }
  
  @Test
  void createItem(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .flatMap(expects(HttpStatus.CREATED))
        .flatMap(parse(TodoDTO.class))
        .unsafeRunSync();

    assertThat(result.title()).isEqualTo("asdfg");
    assertThat(result.url()).isEqualTo("https://tonivade.es/todo/" + result.id());
  }
  
  @Test
  void afterCreateItemThenReturned(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::title).containsExactly("asdfg");
  }
  
  @Test
  void createTwoItems(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .andThen(client.request(createNew("qwert")))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::title).containsExactly("asdfg", "qwert");
  }
  
  @Test
  void updateTitle(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .flatMap(parse(TodoDTO.class))
        .flatMap(item -> client.request(updateTitle(item.id(), "qwert")))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::title).containsExactly("qwert");
  }
  
  @Test
  void updateOrder(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .flatMap(parse(TodoDTO.class))
        .flatMap(item -> client.request(updateOrder(item.id(), 3)))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::order).containsExactly(3);
  }
  
  @Test
  void updateCompleted(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .flatMap(parse(TodoDTO.class))
        .flatMap(item -> client.request(updateCompleted(item.id(), true)))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::completed).containsExactly(true);
  }
  
  @Test
  void updateTitleOrderAndCompleted(UIOMockHttpServer server, UIOHttpClient client) {
    var result = client.request(deleteAll())
        .andThen(client.request(createNew("asdfg")))
        .flatMap(parse(TodoDTO.class))
        .flatMap(item -> client.request(updateTitleOrderAndCompleted(item.id(), "qwert", 3, true)))
        .andThen(client.request(getAll()))
        .flatMap(expects(HttpStatus.OK))
        .flatMap(parseList())
        .unsafeRunSync();
    
    assertThat(result).extracting(TodoDTO::title, TodoDTO::order, TodoDTO::completed).containsExactly(tuple("qwert", 3, true));
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

  private Function1<HttpResponse, UIO<ImmutableList<TodoDTO>>> parseList() {
    return UIOHttpClient.<ImmutableList<TodoDTO>>parse(listOfTodos);
  }

  private Function1<HttpResponse, UIO<HttpResponse>> expects(HttpStatus status) {
    return response -> response.status() != status
        ? UIO.raiseError(new HttpRetryException("error", response.status().code()))
        : UIO.pure(response);
  }
}
