/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.core.Precondition.check;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.purefun.type.Option;

public record Todo(Option<Id> id, Title title, Option<Order> order, State state) {

  public Todo {
    checkNonNull(id, "id cannot be null");
    checkNonNull(title, "title cannot be null");
    checkNonNull(order, "order cannot be null");
    checkNonNull(state, "state cannot be null");
    check(() -> !state.isDraft() || id.isEmpty(), "draft cannot have and non empty id");
    check(() -> state.isDraft() || id.isPresent(), "draft cannot have and non empty id");
  }

  public static Todo draft(String title) {
    return new Todo(Option.none(), new Title(title), Option.none(), State.DRAFT);
  }

  public static Todo draft(String title, int order) {
    return new Todo(Option.none(), new Title(title), Option.some(new Order(order)), State.DRAFT);
  }

  public static Todo create(@Nullable Integer id, String title, Integer order, Boolean completed) {
    return new Todo(
        Option.of(id).map(Id::new),
        new Title(title),
        Option.of(order).map(Order::new),
        completed ? State.COMPLETED : State.NOT_COMPLETED);
  }

  public Todo withId(int id) {
    return new Todo(Option.some(new Id(id)), title, order, State.NOT_COMPLETED);
  }

  public Todo withTitle(String title) {
    return new Todo(id, new Title(title), order, state);
  }

  public Todo withOrder(int order) {
    return new Todo(id, title, Option.some(new Order(order)), state);
  }

  public Todo withCompleted(boolean completed) {
    return new Todo(id, title, order, completed ? State.COMPLETED : State.NOT_COMPLETED);
  }

  public Integer getId() {
    return id.map(Id::value).getOrElseNull();
  }

  public String getTitle() {
    return title.value();
  }

  public Integer getOrder() {
    return order.map(Order::value).getOrElseNull();
  }

  public boolean isCompleted() {
    return state.isCompleted();
  }
}

