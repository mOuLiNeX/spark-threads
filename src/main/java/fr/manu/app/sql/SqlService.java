package fr.manu.app.sql;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.stream.IntStream;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import fr.manu.app.concurrent.EnumComputationStrategy;

public class SqlService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlService.class);
    private final ComboPooledDataSource ds;
    private final DBI dbi;

    static int sqlMaxPoolCapacity = 20;

    static {
        try {
            sqlMaxPoolCapacity = Integer.parseInt(System.getenv("SQL_POOL_CAPACITY"));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid custom SQL_POOL_CAPACITY env var specified, switching to no value", e);
        }
    }

    public SqlService() {
        try {
            this.ds = createDataSource();
            this.dbi = new DBI(ds);

            addShutdownHook();
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }

    }

    private ComboPooledDataSource createDataSource() throws PropertyVetoException {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass("org.postgresql.Driver"); // loads the jdbc driver
        ds.setJdbcUrl("jdbc:postgresql://db/mydb");
        ds.setUser("anonymous");
        ds.setPassword("pwd");
        ds.setMinPoolSize(0);
        ds.setInitialPoolSize(sqlMaxPoolCapacity);
        ds.setMaxPoolSize(sqlMaxPoolCapacity);
        LOGGER.info("Create SQL pool with {} connections", sqlMaxPoolCapacity);
        ds.setMaxStatements(50);
        ds.setMaxIdleTime(300);

        return ds;
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DataSources.destroy(this.ds);
            } catch (SQLException e) {
                System.err.println("Cannot shutdown db connexion on exit : " + e);
            }
        }));
    }

    private void waitSql(int waitTimeInSeconds) {
        Repository repo = dbi.open(Repository.class);
        repo.waitSql(waitTimeInSeconds);
        repo.close();
    }

    public void longSqlComputation(int iterationsCount, int fixedWaitTime, EnumComputationStrategy mode) {
        mode.process(
            IntStream.rangeClosed(1, iterationsCount),
            (() -> this.waitSql(fixedWaitTime)));
    }

}
