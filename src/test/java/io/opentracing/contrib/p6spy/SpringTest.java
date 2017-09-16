package io.opentracing.contrib.p6spy;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SpringTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void init() {
    GlobalTracer.register(mockTracer);
  }

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void test() throws SQLException {
    BasicDataSource dataSource = getDataSource();

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE employee (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(1, finishedSpans.size());
    MockSpan mockSpan = finishedSpans.get(0);

    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals("java-p6spy", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertNotNull(mockSpan.tags().get(Tags.DB_STATEMENT.getKey()));
    assertEquals("hsqldb", mockSpan.tags().get(Tags.DB_TYPE.getKey()));
    assertEquals("SA", mockSpan.tags().get(Tags.DB_USER.getKey()));
    assertEquals(0, mockSpan.generatedErrors().size());

    assertNull(mockTracer.activeSpan());
  }

  private BasicDataSource getDataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:p6spy:hsqldb:mem:spring");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

}
