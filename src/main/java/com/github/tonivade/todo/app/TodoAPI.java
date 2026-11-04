/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.app;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.todo.app.TodoDTO.fromDomain;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Extractors.pathParam;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Tuple3;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;
import com.github.tonivade.zeromock.api.Bytes;
import com.github.tonivade.zeromock.api.Extractors;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.Responses;
import com.github.tonivade.zeromock.api.Serializers;
import java.lang.reflect.Type;

public final class TodoAPI {

  private final TodoRepository<IO<?>> repository;

  private final Type seqOfTodos = new TypeToken<Sequence<TodoDTO>>() {}.getType();

  public TodoAPI(TodoRepository<IO<?>> repository) {
    this.repository = checkNonNull(repository);
  }

  public IO<HttpResponse> cors() {
    return IO.pure(Responses.ok());
  }

  public IO<HttpResponse> create(HttpRequest request) {
    return getTodoDTO(request)
        .flatMap(IO.liftEither(TodoDTO::toDraft))
        .flatMap(repository::create)
        .flatMap(this::serializeTodo)
        .redeem(fromError(), Responses::created);
  }

  public IO<HttpResponse> update(HttpRequest request) {
    return getTodoDTO(request)
        .flatMap(IO.liftEither(TodoDTO::toDomain))
        .flatMap(repository::update)
        .flatMap(IO::fromOption)
        .flatMap(this::serializeTodo)
        .redeem(fromError(), Responses::ok);
  }

  public IO<HttpResponse> modify(HttpRequest request) {
    return getIdAndUpdate(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(repository::modify))
        .flatMap(IO::fromOption)
        .flatMap(this::serializeTodo)
        .redeem(fromError(), Responses::ok);
  }

  public IO<HttpResponse> findAll() {
    return repository.findAll().fix(IOOf::toIO)
        .flatMap(this::serializeTodoList)
        .redeem(fromError(), Responses::ok);
  }

  public IO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(repository::find)
        .flatMap(IO::fromOption)
        .flatMap(this::serializeTodo)
        .redeem(fromError(), Responses::ok);
  }

  public IO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(repository::delete)
        .redeem(fromError(), cons(Responses.ok()));
  }

  public IO<HttpResponse> deleteAll() {
     return repository.deleteAll().fix(IOOf::toIO)
        .redeem(fromError(), cons(Responses.ok()));
  }

  private IO<TodoDTO> getTodoDTO(HttpRequest request) {
    return IO.task(request::body)
        .flatMap(jsonToObject(TodoDTO.class).andThen(IO::fromTry))
        .flatMap(IO::fromOption);
  }

  private IO<Integer> getId(HttpRequest request) {
    return IO.pure(request).map(pathParam(0))
        .map(Integer::parseInt);
  }

  private IO<Option<String>> getTitle(HttpRequest request) {
    return IO.pure(request).map(Extractors.<String>extract("$.title").liftOption());
  }

  private IO<Option<Integer>> getOrder(HttpRequest request) {
    return IO.pure(request).map(Extractors.<Integer>extract("$.order").liftOption());
  }

  private IO<Option<Boolean>> getCompleted(HttpRequest request) {
    return IO.pure(request).map(Extractors.<Boolean>extract("$.completed").liftOption());
  }

  private IO<Tuple2<Integer, Operator1<Todo>>> getIdAndUpdate(HttpRequest request) {
    return IO.tuple(getId(request), getUpdate(request));
  }

  private IO<Operator1<Todo>> getUpdate(HttpRequest request) {
    IO<Tuple3<Operator1<Todo>, Operator1<Todo>, Operator1<Todo>>> map3 = Instances.<IO<?>>applicative()
        .mapN(
            getTitle(request).map(toOperation(Todo::withTitle)),
            getOrder(request).map(toOperation(Todo::withOrder)),
            getCompleted(request).map(toOperation(Todo::withCompleted)))
        .apply(Tuple3::of).fix(IOOf::toIO);
    return map3.map(tuple -> tuple.applyTo((op1, op2, op3) -> op1.andThen(op2).andThen(op3)::apply));
  }

  private <T> Function1<Option<T>, Operator1<Todo>> toOperation(Function2<Todo, T, Todo> function) {
    return value -> value.fold(Producer.cons(todo -> todo), v -> todo -> function.apply(todo, v));
  }

  private Function1<Throwable, HttpResponse> fromError() {
    return error -> switch (error) {
      case IllegalArgumentException e -> Responses.badRequest(e.getMessage());
      default -> Responses.error(error);
    };
  }

  private IO<Bytes> serializeTodoList(Sequence<Todo> todoList) {
    return IO.task(() -> todoList.map(TodoDTO::fromDomain))
        .flatMap(IO.liftTry(Serializers.<Sequence<TodoDTO>>objectToJson(seqOfTodos)));
  }

  private IO<Bytes> serializeTodo(Todo todo) {
    return IO.task(() -> fromDomain(todo))
        .flatMap(IO.liftTry(Serializers.<TodoDTO>objectToJson()));
  }
}
