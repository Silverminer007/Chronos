package de.kjgstbarbara.chronos;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CustomNamingStrategy implements PhysicalNamingStrategy {
    @Override
    public Identifier toPhysicalCatalogName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalColumnName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalSchemaName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalSequenceName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    @Override
    public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    private Identifier convertToSnakeCase(final Identifier identifier) {
        if(identifier == null) {
            return null;
        }
        final String regex = "([a-z])([A-Z])";
        final String replacement = "$1_$2";
        final String newName = identifier.getText()
                .replaceAll(regex, replacement)
                .toLowerCase();
        String prefixedName = newName.startsWith(DATABASE_PREFIX) ? newName : DATABASE_PREFIX + newName;
        return Identifier.toIdentifier(prefixedName);
    }

    private static final String DATABASE_PREFIX = readDataBasePrefix();

    private static String readDataBasePrefix() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("chronos.properties"));
            return properties.getProperty("chronos.database-prefix");
        } catch (IOException e) {
            return "";
        }
    }
}