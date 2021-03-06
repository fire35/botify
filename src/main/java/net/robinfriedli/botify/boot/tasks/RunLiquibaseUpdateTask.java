package net.robinfriedli.botify.boot.tasks;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import net.dv8tion.jda.api.JDA;
import net.robinfriedli.botify.boot.StartupTask;
import net.robinfriedli.botify.entities.xml.StartupTaskContribution;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Startup task that executes changesets defined in the src/main/resources/liquibase/dbchangelog.xml file. Executes all
 * changesets with follwing contexts in this order:
 * <p>
 * 1.: definition  : The context for schema definition changes that create or alter a table or column
 * 2.: initialvalue: The context for statements that insert or remove data
 * 3.: constraint  : The context for adding schema constraints, such as unique constraints
 */
public class RunLiquibaseUpdateTask implements StartupTask {

    private final Logger logger;
    private final SessionFactory sessionFactory;
    private final StartupTaskContribution contribution;

    public RunLiquibaseUpdateTask(SessionFactory sessionFactory, StartupTaskContribution contribution) {
        this.contribution = contribution;
        logger = LoggerFactory.getLogger(getClass());
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void perform(@Nullable JDA shard) {
        logger.info("Beginning liquibase update");
        try (Session session = sessionFactory.openSession()) {
            session.doWork(connection -> {
                try {
                    Database databaseImplementation = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                    Liquibase liquibase = new Liquibase("src/main/resources/liquibase/dbchangelog.xml", new FileSystemResourceAccessor(), databaseImplementation);

                    liquibase.update("definition");
                    liquibase.update("initialvalue");
                    liquibase.update("constraint");
                } catch (LiquibaseException e) {
                    throw new RuntimeException("LiquibaseException occurred", e);
                }
            });
        }
        logger.info("Liquibase update done");
    }

    @Override
    public StartupTaskContribution getContribution() {
        return contribution;
    }
}
