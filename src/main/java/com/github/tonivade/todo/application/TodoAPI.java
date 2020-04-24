package com.github.tonivade.todo.application;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;
import com.github.tonivade.zeromock.api.*;

import java.util.NoSuchElementException;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.effect.Task.task;
import static com.github.tonivade.zeromock.api.Deserializers.jsonToObject;
import static com.github.tonivade.zeromock.api.Headers.contentJson;
import static com.github.tonivade.zeromock.api.Serializers.throwableToJson;
import static java.util.Objects.requireNonNull;

public class TodoAPI {

  private final TodoRepository<Task.µ> repository;

  public TodoAPI(TodoRepository<Task.µ> repository) {
    this.repository = requireNonNull(repository);
  }

  public UIO<HttpResponse> create(HttpRequest request) {
    return getTodo(request)
        .flatMap(todo -> repository.create(todo).fix1(Task::narrowK))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::created))
        .map(contentJson());
  }

  public UIO<HttpResponse> findAll(HttpRequest request) {
    return repository.findAll().fix1(Task::narrowK)
        .fold(fromError(Responses::badRequest), fromSequence(Responses::ok));
  }

  public UIO<HttpResponse> find(HttpRequest request) {
    return getId(request)
        .flatMap(id -> repository.find(id).fix1(Task::narrowK))
        .flatMap(option -> option.fold(this::noSuchElement, Task::pure))
        .fold(fromError(Responses::badRequest), fromTodo(Responses::ok));
  }

  public UIO<HttpResponse> delete(HttpRequest request) {
    return getId(request)
        .flatMap(id -> repository.delete(id).fix1(Task::narrowK))
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  public UIO<HttpResponse> deleteAll(HttpRequest request) {
     return repository.deleteAll().fix1(Task::narrowK)
        .fold(fromError(Responses::badRequest), cons(Responses.ok()));
  }

  private Task<Todo> getTodo(HttpRequest request) {
    return Task.task(request::body)
        .flatMap(Task.lift(jsonToObject(TodoDTO.class)))
        .flatMap(Task.lift(TodoDTO::toDomain));
  }

  private Task<Id> getId(HttpRequest request) {
    return task(() -> request.pathParam(1))
        .flatMap(id -> task(() -> Integer.parseInt(id)))
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
