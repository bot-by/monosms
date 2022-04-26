package uk.bot_by;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import io.undertow.server.HttpHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("fast")
class MonoSMSFastTest {

  @Spy
  public MonoSMS bot;
  @Mock
  HttpHandler handler;

  @DisplayName("Run on the default port")
  @Test
  void defaultPort() {
    // given
    doNothing().when(bot).runOnPort(anyInt());

    // when
    bot.run();

    // then
    verify(bot).runOnPort(8080);
  }

  @DisplayName("Do not wrap if an address list is null")
  @Test
  void notWrapHandler() {
    // given
    String[] allowedAddresses = null;

    // when
    HttpHandler actualHandler = MonoSMS.wrapAddressAccessControl(handler, allowedAddresses);

    // then
    assertEquals(handler, actualHandler, "non-wrapped handler");
  }

  @DisplayName("Wrap handler if an address list is not null")
  @Test
  void wrapHandler() {
    // given
    String[] allowedAddresses = {"127.0.0.1", "192.168.0.1/16"};

    // when
    HttpHandler actualHandler = MonoSMS.wrapAddressAccessControl(handler, allowedAddresses);

    // then
    assertNotEquals(handler, actualHandler, "wrapped handler");
  }

}
