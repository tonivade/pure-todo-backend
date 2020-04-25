/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static java.util.Objects.requireNonNull;

public record Todo(Id id, Title title, Order order, State state) {

  public Todo {
    requireNonNull(id,    "id cannot be null");
    requireNonNull(title, "title cannot be null");
    requireNonNull(order, "order cannot be null");
    requireNonNull(state, "state cannot be null");
    if (state == State.DRAFT && id instanceof Id.Impl) {
      throw new IllegalArgumentException("draft cannot have and non empty id");
    }
  }

  public static Todo draft(String title, Integer order) {
    return new Todo(Id.empty(), new Title(title), new Order(order), State.DRAFT);
  }

  public static Todo create(Integer id, String title, Integer order, Boolean completed) {
    return new Todo(Id.create(id), new Title(title), new Order(order), completed ? State.COMPLETED : State.NOT_COMPLETED);
  }

  public Todo withId(int id) {
    return new Todo(Id.create(id), title, order, State.NOT_COMPLETED);
  }

  public int getId() {
    return id.get();
  }

  public String getTitle() {
    return title.value();
  }

  public int getOrder() {
    return order.value();
  }

  public boolean isCompleted() {
    return state == State.COMPLETED;
  }
}

