/*
 * Copyright (c) 2020-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.effect.Task.liftEither;
import static com.github.tonivade.purefun.effect.Task.liftTry;
import static com.github.tonivade.todo.application.TodoDTO.fromDomain;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Extractors.pathParam;
import static com.github.tonivade.zeromock.api.HttpStatus.BAD_REQUEST;
import static com.github.tonivade.zeromock.api.HttpStatus.INTERNAL_SERVER_ERROR;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Tuple3;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.UIO;
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
import com.github.tonivade.zeromock.api.ProblemDetail;
import com.github.tonivade.zeromock.api.Responses;
import com.github.tonivade.zeromock.api.Serializers;

public final class TodoAPI {

  private static final Logger logger = LoggerFactory.getLogger(TodoAPI.class);

  private final TodoRepository<Task<?>> repository;

  private final Type seqOfTodos = new TypeToken<Sequence<TodoDTO>>() {}.getType();

  public TodoAPI(TodoRepository<Task<?>> repository) {
    this.repository = checkNonNull(repository);
  }

  public UIO<HttpResponse> cors(HttpRequest request) {
    return UIO.pure(Responses.ok());
  }

  public UIO<HttpResponse> create(HttpRequest request) {
    return getTodoDTO(request)
        .flatMap(liftEither(TodoDTO::toDraft))
        .flatMap(repository::create)
        .flatMap(this::serializeTodo)
        .fold(fromError(), Responses::created);
  }

  public UIO<HttpResponse> update(HttpRequest request) {
    return getTodoDTO(request)
        .flatMap(liftEither(TodoDTO::toDomain))
        .flatMap(repository::update)
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(), Responses::ok);
  }

  public UIO<HttpResponse> modify(HttpRequest request) {
    return getIdAndUpdate(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(repository::modify))
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(), Responses::ok);
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix(TaskOf::toTask)
        .flatMap(this::serializeTodoList)
        .fold(fromError(), Responses::ok);
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(repository::find)
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(), Responses::ok);
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(repository::delete)
        .fold(fromError(), cons(Responses.ok()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix(TaskOf::toTask)
        .fold(fromError(), cons(Responses.ok()));
  }

  private Task<TodoDTO> getTodoDTO(HttpRequest request) {
    return Task.task(request::body)
        .flatMap(jsonToObject(TodoDTO.class).andThen(Task::fromTry))
        .flatMap(Task::fromOption);
  }

  private Task<Integer> getId(HttpRequest request) {
    return Task.pure(request).map(pathParam(0))
        .map(Integer::parseInt);
  }

  private Task<Option<String>> getTitle(HttpRequest request) {
    return Task.pure(request).map(Extractors.<String>extract("$.title").liftOption());
  }

  private Task<Option<Integer>> getOrder(HttpRequest request) {
    return Task.pure(request).map(Extractors.<Integer>extract("$.order").liftOption());
  }

  private Task<Option<Boolean>> getCompleted(HttpRequest request) {
    return Task.pure(request).map(Extractors.<Boolean>extract("$.completed").liftOption());
  }

  private Task<Tuple2<Integer, Operator1<Todo>>> getIdAndUpdate(HttpRequest request) {
    return getId(request).zip(getUpdate(request));
  }

  private Task<Operator1<Todo>> getUpdate(HttpRequest request) {
    Task<Tuple3<Operator1<Todo>, Operator1<Todo>, Operator1<Todo>>> map3 = Instances.<Task<?>>applicative()
        .mapN(
            getTitle(request).map(toOperation(Todo::withTitle)),
            getOrder(request).map(toOperation(Todo::withOrder)),
            getCompleted(request).map(toOperation(Todo::withCompleted)),
            Tuple3::of).fix(TaskOf::toTask);
    return map3.map(tuple -> tuple.applyTo((op1, op2, op3) -> op1.andThen(op2).andThen(op3)::apply));
  }

  private <T> Function1<Option<T>, Operator1<Todo>> toOperation(Function2<Todo, T, Todo> function) {
    return value -> value.fold(Producer.cons(todo -> todo), v -> todo -> function.apply(todo, v));
  }

  private Function1<Throwable, HttpResponse> fromError() {
    return error -> {
      logger.error("error", error);
      return switch (error) {
        case IllegalArgumentException e ->
          Responses.from(ProblemDetail.builder(BAD_REQUEST).detail(e.getMessage()).build());
        default ->
          Responses.from(ProblemDetail.builder(INTERNAL_SERVER_ERROR).detail(error.getMessage()).build());
      };
    };
  }

  private Task<Bytes> serializeTodoList(Sequence<Todo> todoList) {
    return Task.task(() -> todoList.map(TodoDTO::fromDomain))
        .flatMap(liftTry(Serializers.<Sequence<TodoDTO>>objectToJson(seqOfTodos)));
  }

  private Task<Bytes> serializeTodo(Todo todo) {
    return Task.task(() -> fromDomain(todo))
        .flatMap(liftTry(Serializers.<TodoDTO>objectToJson()));
  }
}
