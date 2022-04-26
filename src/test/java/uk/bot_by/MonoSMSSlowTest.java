package uk.bot_by;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.security.AbortExecutionException;
import uk.org.webcompere.systemstubs.security.SystemExit;
import uk.org.webcompere.systemstubs.stream.SystemErr;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
@Tag("slow")
class MonoSMSSlowTest {

  @Spy
  MonoSMS application;
  @SystemStub
  private EnvironmentVariables environment;
  @SystemStub
  private SystemExit systemExit;
  @SystemStub
  private SystemErr systemErr;

  @AfterEach
  void tearDown() {
    application.stop();
  }

  @DisplayName("Telegram allowed addresses")
  @Test
  void telegramAllowedAddresses() {
    // given
    environment.set("TELEGRAM_ALLOWED_ADDRESSES", "127.0.0.1;192.168.0.1/16");

    // when and then
    assertDoesNotThrow(() -> MonoSMS.runApplication(application, new String[0]));
  }


  @DisplayName("Viber allowed addresses")
  @Test
  void viberAllowedAddresses() {
    // given
    environment.set("VIBER_ALLOWED_ADDRESSES", "127.0.0.1;192.168.0.1/16");

    // when and then
    assertDoesNotThrow(() -> MonoSMS.runApplication(application, new String[0]));
  }

  @DisplayName("Run with default or special port")
  @ParameterizedTest
  @CsvSource(value = {"NIL", "8080", "5000", "8000;qwerty"}, nullValues = {"NIL"})
  public void happyPath(@ConvertWith(SemicolonArrayConverter.class) String[] args) {
    // when and then
    assertDoesNotThrow(() -> MonoSMS.runApplication(application, args));
  }

  @DisplayName("Wrong arguments")
  @ParameterizedTest
  @CsvSource(value = {"-1,port out of range:-1", "qwerty,For input string: \"qwerty\"",
      "qwerty;8080,For input string: \"qwerty\""})
  void wrongArguments(@ConvertWith(SemicolonArrayConverter.class) String[] args, String message) {
    // given
    String prefix = "\\[main] ERROR uk\\.bot_by\\.MonoSMS - Wrong port number: ";
    String suffix = "[\\n\\r]+";

    // when
    assertThrows(AbortExecutionException.class, () -> MonoSMS.main(args));

    // then
    assertEquals(1, systemExit.getExitCode());
    assertThat(systemErr.getText(), matchesPattern(prefix + message + suffix));
  }

  public static class SemicolonArrayConverter implements ArgumentConverter {

    @Override
    public Object convert(Object source, ParameterContext context)
        throws ArgumentConversionException {
      if (isNull(source)) {
        return new String[0];
      }
      if (source instanceof String) {
        return ((String) source).split(";");
      }
      throw new ArgumentConversionException("could not convert " + source.getClass());
    }

  }

}
