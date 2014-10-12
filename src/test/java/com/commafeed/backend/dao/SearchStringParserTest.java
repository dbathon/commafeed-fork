package com.commafeed.backend.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.commafeed.backend.dao.SearchStringParser.ParsedSearch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

public class SearchStringParserTest {

  private void testWithoutOptions(String searchString, String... terms) {
    final ParsedSearch result = SearchStringParser.parse(searchString);
    assertEquals(ImmutableSet.copyOf(terms), result.terms);
    assertTrue("options not empty", result.options.isEmpty());
  }

  private void testWithOptions(String searchString, SetMultimap<String, String> options,
      String... terms) {
    final ParsedSearch result = SearchStringParser.parse(searchString);
    assertEquals(ImmutableSet.copyOf(terms), result.terms);
    assertEquals(options, result.options);
  }

  @Test
  public void testParser() {
    // empty results
    testWithoutOptions("");
    testWithoutOptions("  \t ");
    testWithoutOptions(null);

    // no quotes
    testWithoutOptions(" just some    terms  ", "just", "some", "terms");
    testWithoutOptions("different terms", "different", "terms");

    testWithoutOptions(" terms 'with quotes '  'and '' quoted Quotes'  'unclosed", "terms",
        "with quotes ", "and ' quoted Quotes", "unclosed");
    testWithoutOptions("' mixed \" ' \"quotes ' \"\" '", " mixed \" ", "quotes ' \" '");
    testWithoutOptions("fbb\" ff'f\" wpeofk 'kk s '  ' \"'' s'  'sss", "fbb ff'f", "wpeofk",
        "kk s ", " \"' s", "sss");

    testWithOptions("a:b a:b a:c b:c foo ", ImmutableSetMultimap.of("a", "b", "a", "c", "b", "c"),
        "foo");
    testWithOptions(
        "  a:'quoted opt'  a:'quoted opt' another:quoted\" \"opt 'no:opt1' no':opt2' n'o:opt3' no-opt:5 ",
        ImmutableSetMultimap.of("a", "quoted opt", "another", "quoted opt"), "no:opt1", "no:opt2",
        "no:opt3", "no-opt:5");
  }

}
