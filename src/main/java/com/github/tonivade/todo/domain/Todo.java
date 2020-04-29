/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import com.github.tonivade.purefun.type.Option;

import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.requireNonNull;

public record Todo(Option<Id> id, Title title, Option<Order> order, State state) {

  public Todo {
    requireNonNull(id, "id cannot be null");
    requireNonNull(title, "title cannot be null");
    requireNonNull(order, "order cannot be null");
    requireNonNull(state, "state cannot be null");
    if (state == State.DRAFT && id.isPresent()) {
      throw new IllegalArgumentException("draft cannot have and non empty id");
    }
    if (state != State.DRAFT && id.isEmpty()) {
      throw new IllegalArgumentException("todo cannot have and empty id");
    }
  }

  public static Todo draft(String title) {
    return new Todo(Option.none(), new Title(title), Option.none(), State.DRAFT);
  }

  public static Todo draft(String title, Integer order) {
    return new Todo(Option.none(), new Title(title), Option.some(new Order(order)), State.DRAFT);
  }

  public static Todo create(Integer id, String title, Integer order, Boolean completed) {
    return new Todo(
        Option.of(id).map(Id::new),
        new Title(title),
        Option.of(order).map(Order::new),
        completed ? State.COMPLETED : State.NOT_COMPLETED);
  }

  public Todo withId(int id) {
    return new Todo(Option.some(new Id(id)), title, order, State.NOT_COMPLETED);
  }

  public Integer getId() {
    return id.fold(cons(null), Id::value);
  }

  public String getTitle() {
    return title.value();
  }

  public Integer getOrder() {
    return order.fold(cons(null), Order::value);
  }

  public boolean isCompleted() {
    return state == State.COMPLETED;
  }
}

