package com.josue.micro.gateway;

import com.josue.micro.registry.client.ServiceStore;
import com.josue.ssr.common.Instance;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Created by Josue on 23/06/2016.
 */
@Path("gateway")
@ApplicationScoped
public class GatewayResource {

    private static final String BALANCE_SERVICE = "balance";
    private static final String BALANCE_SERVICE_RESOURCE = "balances";

    private static final String ACCOUNT_SERVICE = "account";
    private static final String ACCOUNT_SERVICE_RESOURCE = "accounts";

    private static final Logger logger = Logger.getLogger(GatewayResource.class.getName());

    @Inject
    private ServiceStore serviceStore;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String gateway() {
        String response = "Calling '" + BALANCE_SERVICE + "'... response: " + callService(BALANCE_SERVICE, BALANCE_SERVICE_RESOURCE);
        response += "\nCalling '" + ACCOUNT_SERVICE + "'... response: " + callService(ACCOUNT_SERVICE, ACCOUNT_SERVICE_RESOURCE);

        return response;
    }

    @Resource
    private ManagedExecutorService mes;

    @GET
    @Path("async")
    @Produces(MediaType.TEXT_PLAIN)
    public void async(@Suspended final AsyncResponse asyncResponse) {

//        Consumer<Object> consumer = asyncResponse::resume;


        Instance instance = serviceStore.get(ACCOUNT_SERVICE);
        CompletableFuture.supplyAsync(() -> this.callService(ACCOUNT_SERVICE, ACCOUNT_SERVICE_RESOURCE))
                .thenAccept(asyncResponse::resume);


        logger.info("- Before");
        if (instance != null) {
            ClientBuilder.newClient()
                    .target(instance.getAddress()).path(ACCOUNT_SERVICE_RESOURCE)
                    .request()
                    .async()
                    .get(new InvocationCallback<Response>() {
                        @Override
                        public void completed(Response response) {
                            String entity = response.readEntity(String.class);
                            logger.info("- Response received: " + entity);
                            asyncResponse.resume(Response.ok(entity).build());
                        }

                        @Override
                        public void failed(Throwable throwable) {
                            System.out.println("Invocation failed.");
                            throwable.printStackTrace();
                        }
                    });
        }
        logger.info("- After");

    }

    private String callService(String service, String path) {
        Instance instance = serviceStore.get(service);
        if (instance == null) {
            return service.toUpperCase() + "_NOT_AVAIABLE";
        }

        Response response = ClientBuilder.newClient()
                .target(instance.getAddress())
                .path(path)
                .request()
                .get();

        String body = response.readEntity(String.class);

        if (!Response.Status.OK.equals(response.getStatusInfo())) {
            return "REQUEST_ERROR: " + response.getStatus() + " -> " + body;
        }
        return body;
    }

}
