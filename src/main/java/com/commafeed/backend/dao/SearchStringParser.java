package com.commafeed.backend.dao;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

public class SearchStringParser {

  private static final Pattern OPTION_PATTERN = Pattern.compile("(\\w+):(.*)", Pattern.DOTALL);

  public static class Result {
    public static final Result EMPTY_RESULT = new Result(ImmutableSet.of(),
        ImmutableSetMultimap.of());

    public final Set<String> terms;
    public final SetMultimap<String, String> options;

    public Result(Set<String> terms, SetMultimap<String, String> options) {
      this.terms = ImmutableSet.copyOf(terms);
      this.options = ImmutableSetMultimap.copyOf(options);
    }
  }

  private static void processTerm(StringBuilder currentTerm, int firstQuoteIndex,
      Set<String> terms, SetMultimap<String, String> options) {
    final String term = currentTerm.toString();
    final Matcher optionMatcher = OPTION_PATTERN.matcher(term);
    if (optionMatcher.matches()) {
      final String optionKey = optionMatcher.group(1);
      final String optionValue = optionMatcher.group(2);
      if (firstQuoteIndex >= 0 && firstQuoteIndex < optionKey.length() + 1) {
        // if a part of the key (including the colon) is quoted, then it is not an option
        terms.add(term);
      }
      else {
        options.put(optionKey, optionValue);
      }
    }
    else {
      // just a normal term
      terms.add(term);
    }
  }

  public static Result parse(String searchString) {
    if (Strings.isNullOrEmpty(searchString)) {
      return Result.EMPTY_RESULT;
    }

    final Set<String> terms = new LinkedHashSet<>();
    final SetMultimap<String, String> options = HashMultimap.create();

    StringBuilder currentTerm = new StringBuilder();
    boolean inQuotes = false;
    char quoteChar = 0;
    int firstQuoteIndex = -1;

    final int length = searchString.length();
    for (int i = 0; i < length; ++i) {
      final char cur = searchString.charAt(i);
      final char next = i + 1 < length ? searchString.charAt(i + 1) : 0;

      if (Character.isWhitespace(cur)) {
        if (inQuotes) {
          currentTerm.append(cur);
        }
        else if (currentTerm.length() > 0) {
          processTerm(currentTerm, firstQuoteIndex, terms, options);

          // reset
          currentTerm = new StringBuilder();
          firstQuoteIndex = -1;
        }
      }
      else if (cur == '"' || cur == '\'') {
        if (inQuotes) {
          if (cur == quoteChar) {
            if (next == cur) {
              // escaped quotation mark, add and skip next
              currentTerm.append(cur);
              ++i;
            }
            else {
              // end of quotes
              inQuotes = false;
            }
          }
          else {
            currentTerm.append(cur);
          }
        }
        else {
          // start of quotes
          quoteChar = cur;
          inQuotes = true;
          if (firstQuoteIndex < 0) {
            firstQuoteIndex = currentTerm.length();
          }
        }
      }
      else {
        currentTerm.append(cur);
      }
    }

    // add the potential final term
    if (currentTerm.length() > 0) {
      processTerm(currentTerm, firstQuoteIndex, terms, options);
    }

    return new Result(terms, options);
  }

}
