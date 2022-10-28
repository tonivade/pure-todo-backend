/*
 * Copyright (c) 2020-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Validator.equalsTo;
import static com.github.tonivade.purefun.Validator.startsWith;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Validation.invalid;
import static com.github.tonivade.purefun.type.Validation.valid;
import static com.github.tonivade.todo.Application.buildService;
import static com.github.tonivade.todo.Application.loadConfig;
import static com.github.tonivade.zeromock.api.Requests.delete;
import static com.github.tonivade.zeromock.api.Requests.get;
import static com.github.tonivade.zeromock.api.Requests.patch;
import static com.github.tonivade.zeromock.api.Requests.post;
import static com.github.tonivade.zeromock.client.UIOHttpClient.parse;

import java.lang.reflect.Type;
import java.net.HttpRetryException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tonivade.purecheck.spec.UIOTestSpec;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Validator;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.application.TodoDTO;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.client.UIOHttpClient;
import com.github.tonivade.zeromock.junit5.MockHttpServerExtension;
import com.github.tonivade.zeromock.junit5.Mount;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;

@ExtendWith(MockHttpServerExtension.class)
class EndToEndTest extends UIOTestSpec<String> {
  
  private static final String TODO = "/todo";

  final Type listOfTodos = new TypeToken<ImmutableList<TodoDTO>>() {}.getType();
  
  @Mount(TODO)
  final HttpUIOService service = buildService(loadConfig());
  
  @Test
  void test(UIOMockHttpServer server, UIOHttpClient client) {
    
    var suite = suite("Pure Todo Backend End2End",

      it.should("return empty array when empty")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
          .andThen(c.getAll())
          .flatMap(expects(HttpStatus.OK))
          .flatMap(parseList()))
        .thenMustBe(listIsEmpty()),

      it.should("create new items")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(expects(HttpStatus.CREATED))
            .flatMap(parseItem()))
        .thenMustBe(equalsTo("asdfg").compose(TodoDTO::title)
            .andThen(startsWith("https://tonivade.es/todo/").compose(TodoDTO::url))),

      it.should("return new items after created")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::title, "asdfg")),

      it.should("return two items after created")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .andThen(c.createNew("qwert"))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::title, "asdfg", "qwert")),

      it.should("update title")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateTitle(item.id(), "qwert"))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::title, "qwert")),

      it.should("update order")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateOrder(item.id(), 3))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::order, 3)),

      it.should("update completed")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateCompleted(item.id(), true))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::completed, true)),

      it.should("update title, order and completed")
        .given(new TodoClient(client))
        .run(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateTitleOrderAndCompleted(item.id(), "qwert", 3, true))
            .andThen(c.getAll())
            .flatMap(expects(HttpStatus.OK))
            .flatMap(parseList()))
        .thenMustBe(listContainsItems(TodoDTO::completed, true)
            .andThen(listContainsItems(TodoDTO::order, 3)
            .andThen(listContainsItems(TodoDTO::title, "qwert"))))
      
    );
    
    var report = suite.run();
    
    System.out.println(report);
    
    report.assertion();
  }

  @SafeVarargs
  private <T, I> Validator<String, ImmutableList<T>> listContainsItems(Function1<T, I> extractor, I...items) {
    return list -> list.size() == items.length && list.map(extractor).containsAll(listOf(items)) ? valid(list) : invalid("list does not contains items %s".formatted(items));
  }

  private <T> Validator<String, ImmutableList<T>> listIsEmpty() {
    return list -> list.isEmpty() ? valid(list) : invalid("list is not empty");
  }
  
  private static final class TodoClient {
    
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-type";
    
    private final UIOHttpClient client;
    
    public TodoClient(UIOHttpClient client) {
      this.client = checkNonNull(client);
    }

    private UIO<HttpResponse> deleteAll() {
      return client.request(delete(TODO));
    }

    private UIO<HttpResponse> getAll() {
      return client.request(get(TODO));
    }

    private UIO<HttpResponse> createNew(String title) {
      return client.request(post(TODO)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"title":"%s"}
            """.formatted(title)));
    }

    private UIO<HttpResponse> updateTitleOrderAndCompleted(int id, String title, int order, boolean completed) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            { 
                "title": "%s",
                "order": %s,
                "completed": %s
            }
            """.formatted(title, order, completed)));
    }

    private UIO<HttpResponse> updateTitle(int id, String title) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"title": "%s"}
            """.formatted(title)));
    }

    private UIO<HttpResponse> updateOrder(int id, int order) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"order": %s}
            """.formatted(order)));
    }

    private UIO<HttpResponse> updateCompleted(int id, boolean completed) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"completed": %s}
            """.formatted(completed)));
    }
  }

  private Function1<HttpResponse, UIO<ImmutableList<TodoDTO>>> parseList() {
    return UIOHttpClient.<ImmutableList<TodoDTO>>parse(listOfTodos);
  }

  private Function1<HttpResponse, UIO<TodoDTO>> parseItem() {
    return parse(TodoDTO.class);
  }

  private Function1<HttpResponse, UIO<HttpResponse>> expects(HttpStatus status) {
    return response -> response.status() != status
        ? UIO.raiseError(new HttpRetryException("expected status was " + status, response.status().code()))
        : UIO.pure(response);
  }
}
