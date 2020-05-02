/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
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
import static com.github.tonivade.purefun.effect.Task.task;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Headers.contentJson;
import static com.github.tonivade.zeromock.api.Headers.enableCors;
import static com.github.tonivade.zeromock.api.Serializers.throwableToJson;
import static java.util.Objects.requireNonNull;

public final class TodoAPI {

  private final TodoRepository<Task.µ> repository;

  public TodoAPI(TodoRepository<Task.µ> repository) {
    this.repository = requireNonNull(repository);
  }

  public UIO<HttpResponse> cors(HttpRequest request) {
    return UIO.pure(Responses.ok()).map(enableCors());
  }

  public UIO<HttpResponse> create(HttpRequest request) {
    return getTodo(request)
        .flatMap(todo -> repository.create(todo).fix1(Task::narrowK))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::created))
        .map(contentJson().andThen(enableCors()));
  }

  public UIO<HttpResponse> update(HttpRequest request) {
    return getTodo(request)
        .flatMap(todo -> repository.update(todo).fix1(Task::narrowK))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok))
        .map(contentJson().andThen(enableCors()));
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix1(Task::narrowK)
        .fold(fromError(Responses::badRequest), fromSequence(Responses::ok))
        .map(contentJson().andThen(enableCors()));
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .flatMap(id -> repository.find(id).fix1(Task::narrowK))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok))
        .map(contentJson().andThen(enableCors()));
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .flatMap(id -> repository.delete(id).fix1(Task::narrowK))
        .fold(fromError(Responses::badRequest), cons(Responses.ok()))
        .map(contentJson().andThen(enableCors()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix1(Task::narrowK)
        .fold(fromError(Responses::badRequest), cons(Responses.ok()))
        .map(contentJson().andThen(enableCors()));
  }

  private Task<Todo> getTodo(HttpRequest request) {
    return Task.task(request::body)
        .map(jsonToObject(TodoDTO.class))
        .map(TodoDTO::toDomain);
  }

  private Task<Id> getId(HttpRequest request) {
    return task(() -> request.pathParam(0))
        .map(Integer::parseInt)
        .map(Id::new);
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
