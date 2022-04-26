package uk.bot_by;

import static io.undertow.Handlers.ipAccessControl;
import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import handler.TelegramHandler;
import handler.ViberHandler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import java.net.BindException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonoSMS implements Runnable {

  private static final int DEFAULT_PORT_NUMBER = 8080;
  private static final String INDEX_HTML = "index.html";
  private static final String LOCALHOST = "0.0.0.0";
  private static final String PUBLIC = "public";
  private static final String ROOT = "/";
  private static final String SEMICOLON = ";";
  private static final String TELEGRAM = "telegram";
  private static final String TELEGRAM_ADDRESSES = "TELEGRAM_ALLOWED_ADDRESSES";
  private static final String VIBER = "viber";
  private static final String VIBER_ADDRESSES = "VIBER_ALLOWED_ADDRESSES";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Undertow server;

  public static void main(String[] args) {
    try {
      runApplication(new MonoSMS(), args);
    } catch (Exception exception) {
      System.exit(1);
    }
  }

  @VisibleForTesting
  static void runApplication(@NotNull MonoSMS application, @NotNull String[] args)
      throws BindException, IllegalArgumentException, NumberFormatException {
    try {
      Stream.of(args)
          .findFirst()
          .map(Integer::parseInt)
          .ifPresentOrElse(application::runOnPort, application);
    } catch (RuntimeException exception) {
      var cause = (exception.getCause() instanceof RuntimeException)
          ? (RuntimeException) exception.getCause()
          : exception;
      application.logger.error("Wrong port number: {}", cause.getMessage());
      throw cause;
    }
  }

  @VisibleForTesting
  static HttpHandler wrapAddressAccessControl(HttpHandler handler, String[] allowedAddresses) {
    if (isNull(allowedAddresses)) {
      return handler;
    }

    var addressAccessControlHandler = ipAccessControl(handler, false);

    for (String peer : allowedAddresses) {
      addressAccessControlHandler.addAllow(peer);
    }

    return addressAccessControlHandler;
  }

  @Override
  public void run() {
    runOnPort(DEFAULT_PORT_NUMBER);
  }

  @VisibleForTesting
  void runOnPort(int portNumber) {
    PathHandler pathHandler = path().addPrefixPath(ROOT,
        resource(new ClassPathResourceManager(getClass().getClassLoader(), PUBLIC)).setWelcomeFiles(
            INDEX_HTML));

    Optional.ofNullable(System.getenv(TELEGRAM_ADDRESSES))
        .ifPresentOrElse(telegramAllowedAddresses -> pathHandler.addPrefixPath(TELEGRAM,
                wrapAddressAccessControl(new TelegramHandler(),
                    telegramAllowedAddresses.split(SEMICOLON))),
            () -> pathHandler.addPrefixPath(TELEGRAM, new TelegramHandler()));
    Optional.ofNullable(System.getenv(VIBER_ADDRESSES))
        .ifPresentOrElse(telegramAllowedAddresses -> pathHandler.addPrefixPath(VIBER,
                wrapAddressAccessControl(new ViberHandler(),
                    telegramAllowedAddresses.split(SEMICOLON))),
            () -> pathHandler.addPrefixPath(VIBER, new TelegramHandler()));

    server = Undertow.builder()
        .addHttpListener(portNumber, LOCALHOST)
        .setHandler(pathHandler)
        .build();
    server.start();
  }

  @VisibleForTesting
  void stop() {
    if (nonNull(server)) {
      server.stop();
    }
  }

}
