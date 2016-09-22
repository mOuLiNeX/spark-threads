package fr.manu;

import static spark.Spark.exception;
import static spark.Spark.get;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.manu.app.concurrent.EnumComputationStrategy;
import fr.manu.app.sql.SqlService;
import spark.Spark;
import spark.route.RouteOverview;

public class MyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyServer.class);

    static int httpPoolCapacity = -1;
    static EnumComputationStrategy strategy = EnumComputationStrategy.SEQUENTIAL;

    static {
        try {
            httpPoolCapacity = Integer.parseInt(System.getenv("HTTP_POOL_CAPACITY"));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid custom HTTP_POOL_CAPACITY env var specified, switching to no value", e);
        }
        try {
            strategy = EnumComputationStrategy.valueOf(System.getenv("APP_STRATEGY"));
        } catch (Exception e) {
            LOGGER.warn("Invalid custom APP_STRATEGY env var specified, switching to default value (" + strategy + ")", e);
        }
    }

    public static void main(String[] args) {
        RouteOverview.enableRouteOverview();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Spark.stop()));
        if (httpPoolCapacity != -1) {
            Spark.threadPool(httpPoolCapacity);
            LOGGER.info("Positionning Spark HTTP pool with {} threads", httpPoolCapacity);
        }

        get("/ping", (request, response) -> "Pong!");
        final SqlService srv = new SqlService();
        get("/SQL/wait/:iterCount/:waitTime", (request, response) -> {
            int iterCount = Integer.valueOf(request.params(":iterCount"));
            int waitTime = Integer.valueOf(request.params(":waitTime"));
            srv.longSqlComputation(iterCount, waitTime, strategy);

            return "OK";
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.body(e.getMessage());
        });
    }
}
