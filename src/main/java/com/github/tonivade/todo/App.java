/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.purecfg.Source.fromToml;
import static com.github.tonivade.purefun.core.Matcher1.isNotNull;
import static com.github.tonivade.zeromock.api.Headers.contentJson;
import static com.github.tonivade.zeromock.api.Headers.enableCors;
import static com.github.tonivade.zeromock.api.Matchers.jsonPath;
import static com.github.tonivade.zeromock.api.Matchers.options;
import static com.github.tonivade.zeromock.api.Matchers.patch;
import javax.sql.DataSource;

import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation.Result;
import com.github.tonivade.todo.app.TodoAPI;
import com.github.tonivade.todo.infra.TodoDAO;
import com.github.tonivade.todo.infra.TodoDatabaseRepository;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.api.PostFilter;
import com.github.tonivade.zeromock.api.PreFilter;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class App {

  static final String TODO = "/todo";

  public static void main(String[] args) {
    loadConfig()
      .map(config -> buildServer(config).mount(TODO, buildService(config)))
      .getOrElseThrow()
      .start();
  }

  static Validation<Result<String>, Config> loadConfig() {
    return Config.load().validatedRun(fromToml("application.toml"));
  }

  static UIOMockHttpServer buildServer(Config config) {
    return UIOMockHttpServer.builder()
        .host(config.server().host())
        .port(config.server().port())
        .build();
  }

  static HttpUIOService buildService(Config config) {
    var api = new TodoAPI(buildRepository(config));

    return new HttpUIOService("todo backend")
        .preFilter(PreFilter.print(System.out))
        .get("/:id").then(api::find)
        .get("/").then(api::findAll)
        .post("/").then(api::create)
        .put("/:id").then(api::update)
        .when(patch("/:id").and(jsonPath("$.order", isNotNull())
                .or(jsonPath("$.title", isNotNull()))
                .or(jsonPath("$.completed", isNotNull()))))
          .then(api::modify)
        .delete("/:id").then(api::delete)
        .delete("/").then(api::deleteAll)
        .when(options()).then(api::cors)
        .postFilter(enableCors())
        .postFilter(contentJson())
        .postFilter(PostFilter.print(System.out));
  }

  private static TodoDatabaseRepository buildRepository(Config config) {
    var dao = new TodoDAO();
    var dataSource = createDataSource(config.database());

    dao.create().unsafeRun(dataSource);

    return new TodoDatabaseRepository(dao, dataSource);
  }

  private static DataSource createDataSource(Config.Database database) {
    var configuration = new HikariConfig();
    configuration.setJdbcUrl(database.url());
    configuration.setUsername(database.user());
    configuration.setPassword(database.password());
    return new HikariDataSource(configuration);
  }
}