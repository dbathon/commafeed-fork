package com.commafeed.backend;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Type;
import org.hibernate.tool.hbm2ddl.Target;

import com.commafeed.backend.hibernate.CommafeedPostgreSQL9Dialect;

public class SchemaExporter {

  private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([^\\.]+)\\.class");

  private static Stream<Class<?>> getClasses(String packageName) {
    final ClassLoader classLoader = SchemaExporter.class.getClassLoader();
    final URL resource = classLoader.getResource(packageName.replace('.', '/'));
    final File directory = new File(resource.getFile());

    return Arrays.stream(directory.list()).map(fileName -> CLASS_NAME_PATTERN.matcher(fileName))
        .filter(Matcher::matches).map(matcher -> {
          try {
            return Class.forName(packageName + "." + matcher.group(1));
          }
          catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static void exportSchema(final Class<? extends Dialect> dialectClass,
      final String dialectName) {
    final Configuration configuration = new Configuration();
    configuration.setProperty(AvailableSettings.DIALECT, dialectClass.getName());
    configuration.setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");

    getClasses("com.commafeed.backend.model").forEach(
        entityClass -> configuration.addAnnotatedClass(entityClass));

    final SchemaExport schemaExport = new SchemaExport(configuration);
    schemaExport.setDelimiter(";");
    schemaExport.setOutputFile("schema-ddl-" + dialectName + ".sql");
    schemaExport.setFormat(true);
    schemaExport.execute(Target.SCRIPT, Type.CREATE);
  }

  public static void main(String[] args) {
    exportSchema(CommafeedPostgreSQL9Dialect.class, "postgres");
    exportSchema(HSQLDialect.class, "hsql");
  }

}
