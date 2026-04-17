package com.cre.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultContextPostProcessorTest {

  private final DefaultContextPostProcessor processor = new DefaultContextPostProcessor();

  @Test
  void testCollapseMultipleEmptyLines() {
    String input = "line1\n\n\nline2\n\n\n\nline3";
    String result = processor.process(input);
    assertThat(result).isEqualTo("line1\n\nline2\n\nline3");
  }

  @Test
  void testCollapseEmptyLinesWithWhitespace() {
    String input = "line1\n  \n \n\nline2";
    String result = processor.process(input);
    assertThat(result).isEqualTo("line1\n\nline2");
  }

  @Test
  void testFixInlineMarkersPrecededByText() {
    String input = "public class A { <omitted_functions/>\n}";
    String result = processor.process(input);
    // Should be on its own line
    assertThat(result).isEqualTo("public class A {\n<omitted_functions/>\n}");
  }

  @Test
  void testFixInlineMarkersFollowedByText() {
    String input = "public class A {\n  <omitted_functions/> }";
    String result = processor.process(input);
    // Should be on its own line, preserving indentation of the line where marker was
    assertThat(result).isEqualTo("public class A {\n  <omitted_functions/>\n  }");
  }

  @Test
  void testTrimFileBlocks() {
    String input = "<file name=\"A.java\">\n\n  public class A {}\n\n</file>";
    String result = processor.process(input);
    assertThat(result).isEqualTo("<file name=\"A.java\">\n  public class A {}\n</file>");
  }

  @Test
  void testPreserveInternalIndentation() {
    String input = "  public void foo() {\n    if (true) {\n      System.out.println(\"hi\");\n    }\n  }";
    String result = processor.process(input);
    // Should NOT be trimmed line by line
    assertThat(result).isEqualTo(input);
  }

  @Test
  void testMixedScenario() {
    String input = """
        <file name="A.java">
        
        public class A {
          <omitted_properties/> public void foo() {}
        
        
        
        }
        
        </file>
        """;
    String result = processor.process(input);
    // The "public void foo()" line should inherit the 2 spaces from the split line
    String expected = """
        <file name="A.java">
        public class A {
          <omitted_properties/>
          public void foo() {}
        
        }
        </file>
        """.trim();
    assertThat(result).isEqualTo(expected);
  }
}
