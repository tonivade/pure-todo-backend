package com.github.tonivade.todo;

import com.github.tonivade.todo.application.TodoAPI;
import com.github.tonivade.todo.infrastructure.TodoInMemoryRepository;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;

import static com.github.tonivade.zeromock.api.Matchers.delete;
import static com.github.tonivade.zeromock.api.Matchers.get;
import static com.github.tonivade.zeromock.api.Matchers.post;
import static com.github.tonivade.zeromock.api.Matchers.put;

public class Application {

  public static void main(String[] args) {
    var config = Config.load("application.properties").getOrElseThrow();
    var repository = new TodoInMemoryRepository();
    var api = new TodoAPI(repository);
    var service = new HttpUIOService("todo backend")
        .when(get("/:id")).then(api::find)
        .when(get("/")).then(api::findAll)
        .when(post("/")).then(api::create)
        .when(put("/:id")).then(api::update)
        .when(delete("/:id")).then(api::delete)
        .when(delete("/")).then(api::deleteAll).build();
    var server = UIOMockHttpServer.async()
        .host(config.server().host())
        .port(config.server().port()).build();
    server.mount("/todo", service).start();
  }
}