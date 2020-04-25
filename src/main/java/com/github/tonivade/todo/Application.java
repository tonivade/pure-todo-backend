/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import com.github.tonivade.todo.application.TodoAPI;
import com.github.tonivade.todo.infrastructure.TodoDAO;
import com.github.tonivade.todo.infrastructure.TodoDatabaseRepository;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static com.github.tonivade.zeromock.api.Matchers.delete;
import static com.github.tonivade.zeromock.api.Matchers.get;
import static com.github.tonivade.zeromock.api.Matchers.post;
import static com.github.tonivade.zeromock.api.Matchers.put;

public class Application {

  public static void main(String[] args) {
    var config = Config.load("application.properties").getOrElseThrow();

    var dao = new TodoDAO();
    HikariConfig configuration = new HikariConfig();
    configuration.setJdbcUrl(config.database().url());
    configuration.setUsername(config.database().user());
    configuration.setPassword(config.database().password());
    var dataSource = new HikariDataSource(configuration);

    dao.create().unsafeRun(dataSource);

    var repository = new TodoDatabaseRepository(dao, dataSource);
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