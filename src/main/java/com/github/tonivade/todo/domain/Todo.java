package com.github.tonivade.todo.domain;

import static java.util.Objects.requireNonNull;

public record Todo(Id id, Title title, Order order, State state) {

  public Todo {
    requireNonNull(id,    () -> "id cannot be null");
    requireNonNull(title, () -> "title cannot be null");
    requireNonNull(order, () -> "order cannot be null");
    requireNonNull(state, () -> "state cannot be null");
  }

  public static Todo create(Integer id, String title, Integer order, Boolean completed) {
    return new Todo(new Id(id), new Title(title), new Order(order), completed ? State.COMPLETED : State.NOT_COMPLETED);
  }

  public int getId() {
    return id.value();
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

