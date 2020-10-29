/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Extractors.pathParam;
import static com.github.tonivade.zeromock.api.Serializers.throwableToJson;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import com.github.tonivade.json.Json;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Tuple3;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;
import com.github.tonivade.zeromock.api.Bytes;
import com.github.tonivade.zeromock.api.Extractors;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.Responses;
import com.github.tonivade.zeromock.api.Serializers;
import com.google.gson.reflect.TypeToken;

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
        .flatMap(todo -> repository.create(todo).fix(toTask()))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::created));
  }

  public UIO<HttpResponse> update(HttpRequest request) {
    return getTodo(request)
        .map(TodoDTO::toDomain)
        .flatMap(todo -> repository.update(todo).fix(toTask()))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> modify(HttpRequest request) {
    return getIdAndUpdate(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(
            (id, update) -> repository.modify(id, update::apply).fix(toTask())))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix(toTask())
        .fold(fromError(Responses::badRequest), fromSequence(Responses::ok));
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.find(id).fix(toTask()))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.delete(id).fix(toTask()))
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix(toTask())
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  private Task<TodoDTO> getTodo(HttpRequest request) {
    return Task.task(request::body)
        .map(jsonToObject(json -> new Json().fromJson(json, TodoDTO.class)));
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
    return Task.map2(getId(request), getUpdate(request), Tuple2::of);
  }

  private Task<Operator1<Todo>> getUpdate(HttpRequest request) {
    Task<Tuple3<Operator1<Todo>, Operator1<Todo>, Operator1<Todo>>> map3 = TaskInstances.applicative()
        .mapN(
            getTitle(request).map(toOperation(Todo::withTitle)), 
            getOrder(request).map(toOperation(Todo::withOrder)), 
            getCompleted(request).map(toOperation(Todo::withCompleted)), 
            Tuple3::of).fix(toTask());
    return map3.map(tuple -> tuple.applyTo((op1, op2, op3) -> op1.andThen(op2).andThen(op3)::apply));
  }
  
  private <T> Function1<Option<T>, Operator1<Todo>> toOperation(Function2<Todo, T, Todo> function) {
    return value -> value.fold(Producer.cons(todo -> todo), v -> todo -> function.apply(todo, v));
  }

  private Function1<Throwable, HttpResponse> fromError(Function1<Bytes, HttpResponse> toResponse) {
    return throwableToJson().andThen(toResponse);
  }

  private Function1<Todo, HttpResponse> fromTodo(Function1<Bytes, HttpResponse> toResponse) {
    return Serializers.<TodoDTO>objectToJson(new Json()::toString)
        .compose(TodoDTO::fromDomain)
        .andThen(toResponse);
  }

  private Function1<Sequence<Todo>, HttpResponse> fromSequence(Function1<Bytes, HttpResponse> toResponse) {
    Type listOfTodos = new TypeToken<Sequence<TodoDTO>>() {}.getType();
    return Serializers.<Sequence<TodoDTO>>objectToJson(value -> new Json().toString(value, listOfTodos))
        .<Sequence<Todo>>compose(seq -> seq.map(TodoDTO::fromDomain))
        .andThen(toResponse);
  }

  private <X> Task<X> noSuchElement() {
    return Task.raiseError(new NoSuchElementException());
  }
}
