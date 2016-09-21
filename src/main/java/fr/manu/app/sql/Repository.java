package fr.manu.app.sql;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface Repository {
    @SqlUpdate("SELECT pg_sleep(:seconds)")
    void waitSql(@Bind("seconds") int waitTimeInSeconds);

    /**
     * close with no args is used to close the connection
     */
    void close();
}
