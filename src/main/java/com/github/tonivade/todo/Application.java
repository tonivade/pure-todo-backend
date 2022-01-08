/*
 * Copyright (c) 2020-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.purefun.Matcher1.isNotNull;
import static com.github.tonivade.zeromock.api.Headers.contentJson;
import static com.github.tonivade.zeromock.api.Headers.enableCors;
import static com.github.tonivade.zeromock.api.Matchers.delete;
import static com.github.tonivade.zeromock.api.Matchers.get;
import static com.github.tonivade.zeromock.api.Matchers.jsonPath;
import static com.github.tonivade.zeromock.api.Matchers.options;
import static com.github.tonivade.zeromock.api.Matchers.patch;
import static com.github.tonivade.zeromock.api.Matchers.post;
import static com.github.tonivade.zeromock.api.Matchers.put;

import javax.sql.DataSource;

import com.github.tonivade.todo.application.TodoAPI;
import com.github.tonivade.todo.infrastructure.TodoDAO;
import com.github.tonivade.todo.infrastructure.TodoDatabaseRepository;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.api.PostFilter;
import com.github.tonivade.zeromock.api.PreFilter;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class Application {

  public static void main(String[] args) {
    var config = loadConfig();

    buildServer(config, buildService(config)).start();
  }

  protected static Config loadConfig() {
    return Config.load("application.toml").getOrElseThrow();
  }

  protected static UIOMockHttpServer buildServer(Config config, HttpUIOService service) {
    var server = UIOMockHttpServer.builder()
        .host(config.server().host())
        .port(config.server().port()).build();
    return server.mount("/todo", service);
  }

  protected static HttpUIOService buildService(Config config) {
    var dao = new TodoDAO();
    var dataSource = createDataSource(config);

    dao.create().unsafeRun(dataSource);

    var repository = new TodoDatabaseRepository(dao, dataSource);
    var api = new TodoAPI(repository);

    return new HttpUIOService("todo backend")
        .preFilter(PreFilter.print(System.out))
        .when(get("/:id")).then(api::find)
        .when(get("/")).then(api::findAll)
        .when(post("/")).then(api::create)
        .when(put("/:id")).then(api::update)
        .when(patch("/:id").and(jsonPath("$.order", isNotNull())
                .or(jsonPath("$.title", isNotNull()))
                .or(jsonPath("$.completed", isNotNull()))))
          .then(api::modify)
        .when(delete("/:id")).then(api::delete)
        .when(delete("/")).then(api::deleteAll)
        .when(options()).then(api::cors)
        .postFilter(enableCors())
        .postFilter(contentJson())
        .postFilter(PostFilter.print(System.out));
  }

  private static DataSource createDataSource(Config config) {
    var configuration = new HikariConfig();
    configuration.setJdbcUrl(config.database().url());
    configuration.setUsername(config.database().user());
    configuration.setPassword(config.database().password());
    return new HikariDataSource(configuration);
  }
}