package com.github.tonivade.todo;

import com.github.tonivade.todo.domain.Todo;

public class Application {
  public static void main(String[] args) {
    // TODO
    var item = Todo.create(1, "title", 1, false);

    System.out.println(item);
  }
}