package services;

import java.sql.Connection;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.Database;

/**
 * The ONLY class that opens and closes database connections.
 * Services ask this class for a connection; repositories just receive it.
 */
@Singleton
public class Common {

    private static final Logger log = LoggerFactory.getLogger(Common.class);
    public static final String MSG_EQUIPMENT_MISSING = "The selected equipment does not exist.";
    public static final String MSG_RECORD_NOT_FOUND = "Equipment record not found";


    /** A block of database work that uses one Connection. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final Database db;

    @Inject
    public Common(Database db) {
        this.db = db;
    }

    /* ---------- open / close ---------- */

    /** Opens a connection from the pool. Each statement is saved immediately (autocommit on). */
    public Connection openConnection() throws SQLException {
        return db.getConnection(true);
    }

    /** Opens a connection for a transaction. Nothing is saved until commit(). */
    public Connection openTransaction() throws SQLException {
        return db.getConnection(false);
    }

    public void commit(Connection connection) throws SQLException {
        connection.commit();
    }

    /** Never throws: a failed rollback must not hide the original error. */
    // public void rollback(Connection connection) {
    //     if (connection == null) {
    //         return;
    //     }
    //     try {
    //         connection.rollback();
    //     } catch (SQLException e) {
    //         log.error("Rollback failed", e);
    //     }
    // }

    /** Null-safe. For a pooled connection, close() returns it to the pool. */
    public void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.error("Closing the connection failed", e);
        }
    }

    /* ---------- ready-made patterns for services ---------- */

    // /** For reads: open, run, always close. */
    // public <T> T inConnection(SqlWork<T> work) {
    //     Connection connection = null;
    //     try {
    //         connection = openConnection();
    //         return work.run(connection);
    //     } catch (SQLException e) {
    //         throw new RuntimeException("Database error: " + e.getMessage(), e);
    //     } finally {
    //         closeConnection(connection);
    //     }
    // }

    /** For writes: open, run, commit. On any error roll back; always close. */
    // public <T> T inTransaction(SqlWork<T> work) {
    //     Connection connection = null;
    //     try {
    //         connection = openTransaction();
    //         T result = work.run(connection);
    //         commit(connection);
    //         return result;
    //     } catch (SQLException e) {
    //         rollback(connection);
    //         throw new RuntimeException("Database error: " + e.getMessage(), e);
    //     } catch (RuntimeException e) {
    //         rollback(connection);          // e.g. DuplicateResourceException, ValidationException
    //         throw e;                       // unchanged, so your error handler still returns 409/400/404
    //     } finally {
    //         closeConnection(connection);
    //     }
    // }
}