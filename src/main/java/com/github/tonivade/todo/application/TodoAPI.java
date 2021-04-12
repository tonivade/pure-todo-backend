/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import static com.github.tonivade.todo.application.TodoDTO.fromDomain;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Extractors.pathParam;
import static com.github.tonivade.zeromock.api.Serializers.objectToJson;
import static com.github.tonivade.zeromock.api.Serializers.throwableToJson;

import java.lang.reflect.Type;

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
import com.github.tonivade.purejson.TypeToken;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;
import com.github.tonivade.zeromock.api.Bytes;
import com.github.tonivade.zeromock.api.Extractors;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.Responses;

public final class TodoAPI {

  private final TodoRepository<Task_> repository;

  public TodoAPI(TodoRepository<Task_> repository) {
    this.repository = checkNonNull(repository);
  }

  public UIO<HttpResponse> cors(HttpRequest request) {
    return UIO.pure(Responses.ok());
  }

  public UIO<HttpResponse> create(HttpRequest request) {
    return getTodoDTO(request)
        .map(TodoDTO::toDraft)
        .flatMap(todo -> repository.create(todo))
        .flatMap(this::serializeTodo)
        .fold(fromError(Responses::badRequest), Responses::created);
  }

  public UIO<HttpResponse> update(HttpRequest request) {
    return getTodoDTO(request)
        .map(TodoDTO::toDomain)
        .flatMap(todo -> repository.update(todo))
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(Responses::badRequest), Responses::ok);
  }

  public UIO<HttpResponse> modify(HttpRequest request) {
    return getIdAndUpdate(request)
        .flatMap(tuple -> tuple.map1(Id::new).applyTo(
            (id, update) -> repository.modify(id, update::apply)))
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(Responses::badRequest), Responses::ok);
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix(toTask())
        .flatMap(this::serializeTodoList)
        .fold(fromError(Responses::badRequest), Responses::ok);
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.find(id))
        .flatMap(Task::fromOption)
        .flatMap(this::serializeTodo)
        .fold(fromError(Responses::badRequest), Responses::ok);
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .map(Id::new)
        .flatMap(id -> repository.delete(id))
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix(toTask())
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
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
    return throwableToJson().andThen(t -> t.fold(Responses::error, toResponse));
  }
  
  private Task<Bytes> serializeTodoList(Sequence<Todo> todoList) {
    Type seqOfTodos = new TypeToken<Sequence<TodoDTO>>() {}.getType();
    return Task.task(() -> todoList.map(TodoDTO::fromDomain))
        .flatMap(objectToJson(seqOfTodos).andThen(Task::fromTry));
  }
  
  private Task<Bytes> serializeTodo(Todo todo) {
    return Task.task(() -> fromDomain(todo))
        .flatMap(objectToJson(TodoDTO.class).andThen(Task::fromTry));
  }
}
