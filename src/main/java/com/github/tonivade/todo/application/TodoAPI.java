/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;
import com.github.tonivade.zeromock.api.Bytes;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.Responses;
import com.github.tonivade.zeromock.api.Serializers;

import java.util.NoSuchElementException;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Extractors.extract;
import static com.github.tonivade.zeromock.api.Extractors.pathParam;
import static com.github.tonivade.zeromock.api.Serializers.throwableToJson;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

public final class TodoAPI {

  private final TodoRepository<Task_> repository;

  public TodoAPI(TodoRepository<Task_> repository) {
    this.repository = checkNonNull(repository);
  }

  public UIO<HttpResponse> cors(HttpRequest request) {
    return UIO.pure(Responses.ok());
  }

  public UIO<HttpResponse> create(HttpRequest request) {
    return getTodo(request)
        .map(TodoDTO::toDraft)
        .flatMap(todo -> repository.create(todo).fix(TaskOf::narrowK))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::created));
  }

  public UIO<HttpResponse> update(HttpRequest request) {
    return getTodo(request)
        .map(TodoDTO::toDomain)
        .flatMap(todo -> repository.update(todo).fix(TaskOf::narrowK))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> updateTitle(HttpRequest request) {
    return getIdAndTitle(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(
            (id, title) -> repository.updateTitle(id, title).fix(TaskOf::narrowK)))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> updateOrder(HttpRequest request) {
    return getIdAndOrder(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(
            (id, order) -> repository.updateOrder(id, order).fix(TaskOf::narrowK)))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> updateCompleted(HttpRequest request) {
    return getIdAndCompleted(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(
            (id, completed) -> repository.updateCompleted(id, completed).fix(TaskOf::narrowK)))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix(TaskOf::narrowK)
        .fold(fromError(Responses::badRequest), fromSequence(Responses::ok));
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.find(id).fix(TaskOf::narrowK))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.delete(id).fix(TaskOf::narrowK))
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix(TaskOf::narrowK)
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  private Task<TodoDTO> getTodo(HttpRequest request) {
    return Task.task(request::body)
        .map(jsonToObject(TodoDTO.class));
  }

  private Task<Integer> getId(HttpRequest request) {
    return Task.pure(request).map(pathParam(0))
        .map(Integer::parseInt);
  }

  private Task<String> getTitle(HttpRequest request) {
    return Task.pure(request).map(extract("$.title"));
  }

  private Task<Integer> getOrder(HttpRequest request) {
    return Task.pure(request).map(extract("$.order"));
  }

  private Task<Boolean> getCompleted(HttpRequest request) {
    return Task.pure(request).map(extract("$.completed"));
  }

  private Task<Tuple2<Integer, String>> getIdAndTitle(HttpRequest request) {
    return Task.map2(getId(request), getTitle(request), Tuple2::of);
  }

  private Task<Tuple2<Integer, Integer>> getIdAndOrder(HttpRequest request) {
    return Task.map2(getId(request), getOrder(request), Tuple2::of);
  }

  private Task<Tuple2<Integer, Boolean>> getIdAndCompleted(HttpRequest request) {
    return Task.map2(getId(request), getCompleted(request), Tuple2::of);
  }

  private Function1<Throwable, HttpResponse> fromError(Function1<Bytes, HttpResponse> toResponse) {
    return throwableToJson().andThen(toResponse);
  }

  private Function1<Todo, HttpResponse> fromTodo(Function1<Bytes, HttpResponse> toResponse) {
    return Serializers.<TodoDTO>objectToJson()
        .compose(TodoDTO::fromDomain)
        .andThen(toResponse);
  }

  private Function1<Sequence<Todo>, HttpResponse> fromSequence(Function1<Bytes, HttpResponse> toResponse) {
    return Serializers.<Sequence<TodoDTO>>objectToJson()
        .<Sequence<Todo>>compose(seq -> seq.map(TodoDTO::fromDomain))
        .andThen(toResponse);
  }

  private <X> Task<X> noSuchElement() {
    return Task.raiseError(new NoSuchElementException());
  }
}
