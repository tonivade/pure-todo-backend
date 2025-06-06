/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import com.github.tonivade.purecfg.PureCFG;
import static com.github.tonivade.purecfg.PureCFG.mapN;
import static com.github.tonivade.purecfg.PureCFG.readConfig;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Precondition.checkRange;

public record Config(Server server, Database database) {

  public Config {
    checkNonNull(server);
    checkNonNull(database);
  }

  public static PureCFG<Config> load() {
    return mapN(
        readConfig("server", Server.load()),
        readConfig("database", Database.load())).apply(Config::new);
  }

  public record Server(String host, Integer port) {

    public Server {
      checkNonNull(host);
      checkNonNull(port);
      checkRange(port, 1024, 65535);
    }

    public static PureCFG<Server> load() {
      return mapN(readString("host"), readInt("port")).apply(Server::new);
    }
  }

  public record Database(String url, String user, String password) {

    public Database {
      checkNonNull(url);
      checkNonNull(user);
      checkNonNull(password);
    }

    public static PureCFG<Database> load() {
      return mapN(readString("url"), readString("user"), readString("password")).apply(Database::new);
    }
  }
}
