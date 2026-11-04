/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Validator.equalsTo;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Validation.invalid;
import static com.github.tonivade.purefun.type.Validation.valid;
import static com.github.tonivade.todo.App.TODO;
import static com.github.tonivade.zeromock.api.HttpStatus.BAD_REQUEST;
import static com.github.tonivade.zeromock.api.HttpStatus.CREATED;
import static com.github.tonivade.zeromock.api.HttpStatus.OK;
import static com.github.tonivade.zeromock.api.Requests.delete;
import static com.github.tonivade.zeromock.api.Requests.get;
import static com.github.tonivade.zeromock.api.Requests.patch;
import static com.github.tonivade.zeromock.api.Requests.post;
import java.lang.reflect.Type;
import java.net.HttpRetryException;

import org.junit.jupiter.api.Test;
import com.github.tonivade.purecheck.spec.IOTestSpec;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Validator;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.app.TodoDTO;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import com.github.tonivade.zeromock.api.HttpIOService;
import com.github.tonivade.zeromock.client.IOHttpClient;
import com.github.tonivade.zeromock.junit5.Mount;
import com.github.tonivade.zeromock.junit5.Zeromock;
import com.github.tonivade.zeromock.server.IOMockHttpServer;

@Zeromock(type = IOMockHttpServer.class)
class EndToEndTest extends IOTestSpec<String> {

  final Type listOfTodos = new TypeToken<ImmutableList<TodoDTO>>() {}.getType();

  @Mount(TODO)
  final HttpIOService service = App.loadConfig().map(App::buildService).getOrElseThrow();

  @Test
  void test(IOMockHttpServer server, IOHttpClient client) {
    var todoClient = new TodoClient(client);

    var suite = suite("Pure Todo Backend End2End",

      it.should("return empty array when empty")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
          .andThen(c.getAll())
          .flatMap(expects(OK))
          .flatMap(parseList()))
        .then(listIsEmpty()),

      it.should("create new items")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(expects(CREATED))
            .flatMap(parseItem()))
        .then(equalsTo("asdfg").compose(TodoDTO::title)
            .andThen(urlShouldBeValid())),

      it.should("return new items after created")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::title, "asdfg")),

      it.should("return two items after created")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .andThen(c.createNew("qwert"))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::title, "asdfg", "qwert")),

      it.should("update title")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateTitle(item.id(), "qwert"))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::title, "qwert")),

      it.should("update order")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateOrder(item.id(), 3))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::order, 3)),

      it.should("update completed")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateCompleted(item.id(), true))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::completed, true)),

      it.should("update title, order and completed")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew("asdfg"))
            .flatMap(parseItem())
            .flatMap(item -> c.updateTitleOrderAndCompleted(item.id(), "qwert", 3, true))
            .andThen(c.getAll())
            .flatMap(expects(OK))
            .flatMap(parseList()))
        .then(listContainsItems(TodoDTO::completed, true)
            .andThen(listContainsItems(TodoDTO::order, 3)
            .andThen(listContainsItems(TodoDTO::title, "qwert")))),

      it.should("fail if no title")
        .given(todoClient)
        .whenK(c -> c.deleteAll()
            .andThen(c.createNew()))
        .then(equalsTo(BAD_REQUEST).compose(HttpResponse::status))

    );

    var report = suite.run();

    System.out.println(report);

    report.assertion();
  }

  private Validator<String, TodoDTO> urlShouldBeValid() {
    return dto -> dto.url().equals("https://tonivade.es" + TODO + "/" + dto.id()) ? valid(dto) : invalid("url not valid: " + dto.url());
  }

  @SafeVarargs
  private <T, I> Validator<String, ImmutableList<T>> listContainsItems(Function1<T, I> extractor, I...items) {
    return Validator.from(
        list -> list.size() == items.length && list.map(extractor).containsAll(listOf(items)),
        () -> "list does not contains items %s".formatted(items));
  }

  private <T> Validator<String, ImmutableList<T>> listIsEmpty() {
    return Validator.from(ImmutableList::isEmpty, () -> "list is not empty");
  }

  private static final class TodoClient {

    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-type";

    private final IOHttpClient client;

    public TodoClient(IOHttpClient client) {
      this.client = checkNonNull(client);
    }

    private IO<HttpResponse> deleteAll() {
      return client.request(delete(TODO));
    }

    private IO<HttpResponse> getAll() {
      return client.request(get(TODO));
    }

    private IO<HttpResponse> createNew(String title) {
      return client.request(post(TODO)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"title":"%s"}
            """.formatted(title)));
    }

    private IO<HttpResponse> createNew() {
      return client.request(post(TODO)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {}
            """));
    }

    private IO<HttpResponse> updateTitleOrderAndCompleted(int id, String title, int order, boolean completed) {
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

    private IO<HttpResponse> updateTitle(int id, String title) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"title": "%s"}
            """.formatted(title)));
    }

    private IO<HttpResponse> updateOrder(int id, int order) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"order": %s}
            """.formatted(order)));
    }

    private IO<HttpResponse> updateCompleted(int id, boolean completed) {
      return client.request(patch(TODO + "/" + id)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(
            """
            {"completed": %s}
            """.formatted(completed)));
    }

    @Override
    public String toString() {
      return "TodoClient";
    }
  }

  private Function1<HttpResponse, IO<ImmutableList<TodoDTO>>> parseList() {
    return IOHttpClient.<ImmutableList<TodoDTO>>parse(listOfTodos);
  }

  private Function1<HttpResponse, IO<TodoDTO>> parseItem() {
    return IOHttpClient.<TodoDTO>parse();
  }

  private Function1<HttpResponse, IO<HttpResponse>> expects(HttpStatus status) {
    return response -> response.status() != status
        ? IO.raiseError(new HttpRetryException("expected status was " + status, response.status().code()))
        : IO.pure(response);
  }
}
