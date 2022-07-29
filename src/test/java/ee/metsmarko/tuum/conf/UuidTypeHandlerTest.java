package ee.metsmarko.tuum.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidTypeHandlerTest {
  UuidTypeHandler typeHandler = new UuidTypeHandler();
  private final ResultSet rs = mock(ResultSet.class);
  private final CallableStatement cs = mock(CallableStatement.class);
  private final PreparedStatement ps = mock(PreparedStatement.class);
  private final UUID uuid = UUID.randomUUID();

  @Test
  public void shouldSetParameter() throws Exception {
    typeHandler.setParameter(ps, 1, uuid, null);

    verify(ps).setObject(1, uuid);
  }

  @Test
  void testColumnName() throws SQLException {
    when(rs.getObject("column", UUID.class)).thenReturn(uuid);

    assertEquals(uuid, typeHandler.getResult(rs, "column"));
  }

  @Test
  void testColumnIndex() throws SQLException {
    when(rs.getObject(1, UUID.class)).thenReturn(uuid);

    assertEquals(uuid, typeHandler.getResult(rs, 1));
  }

  @Test
  void testCallableStatement() throws SQLException {
    when(cs.getObject(1, UUID.class)).thenReturn(uuid);

    assertEquals(uuid, typeHandler.getResult(cs, 1));
  }
}
