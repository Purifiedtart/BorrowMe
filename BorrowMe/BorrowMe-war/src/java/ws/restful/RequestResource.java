package ws.restful;
//POJO CLASS

import ejb.session.stateless.ListingSessionBeanLocal;
import ejb.session.stateless.RequestSessionBeanLocal;
import entity.ListingEntity;
import entity.RequestEntity;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import util.exception.CreateRequestException;
import util.exception.CustomerNotFoundException;
import util.exception.InvalidListingException;
import util.exception.RequestNotFoundException;
import ws.restful.datamodel.Request.CreateRequestReq;
import ws.restful.datamodel.Request.CreateRequestRsp;
import ws.restful.datamodel.Listing.ErrorRsp;
import ws.restful.datamodel.Request.DeleteRequestRsp;
import ws.restful.datamodel.Request.RequestMadeRsp;

@Path("Request") //demarcate the URI to identify resource

public class RequestResource {

    ListingSessionBeanLocal listingSessionBeanLocal = lookupListingSessionBeanLocal();

    RequestSessionBeanLocal requestSessionBeanLocal = lookupRequestSessionBeanLocal();

    @Context
    private UriInfo context;

    public RequestResource() {
        this.requestSessionBeanLocal = lookupRequestSessionBeanLocal();
        this.listingSessionBeanLocal = lookupListingSessionBeanLocal();
    }

    @Path("{requesterId}/{listingId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRequest(JAXBElement<CreateRequestReq> jaxbCreateRequestReq,
            @PathParam("requesterId") Long requesterId,
            @PathParam("listingId") Long listingId) {
        if ((jaxbCreateRequestReq != null) && (jaxbCreateRequestReq.getValue() != null)) {

            try {
                CreateRequestReq createRequestReq = jaxbCreateRequestReq.getValue();

                RequestEntity rq = requestSessionBeanLocal.createRequestAPI(createRequestReq.getRequest(), requesterId, listingId, createRequestReq.getStartDateStr(), createRequestReq.getEndDateStr());
                if (rq == null) {
                    System.out.println("Response was null");
                    return Response.status(Response.Status.OK).entity(null).build();
                } else {
                    System.out.println("Reponse was not null");
                    CreateRequestRsp rsp = new CreateRequestRsp(rq);
                    return Response.status(Response.Status.OK).entity(rsp).build();
                }
            } catch (CreateRequestException | InvalidListingException ex) {
                ErrorRsp errorRsp = new ErrorRsp("Unable to create request");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            }

        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid create request request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    @Path("requestMade/{customerId}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestMade(@PathParam("customerId") Long customerId) {
        List<RequestEntity> requests = requestSessionBeanLocal.retrieveRequestListByCustomerID(customerId);
        for (int i = 0; i < requests.size(); i++) {
            requests.get(i).getFeedbackList().size();
        }
        RequestMadeRsp reqMadeRsp = new RequestMadeRsp(requests);
        return Response.status(Response.Status.OK).entity(reqMadeRsp).build();
    }

    @Path("requestReceived/{customerId}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestReceived(@PathParam("customerId") Long customerId) {
        try {
            List<ListingEntity> listings = listingSessionBeanLocal.retrieveListingByCustomerId(customerId);
            System.out.println("*************Listing size: " + listings.size() + "********************");
            List<RequestEntity> requests = new ArrayList<>();
            for (int i = 0; i < listings.size(); i++) {
                ListingEntity singleListing = listings.get(i);
                List<RequestEntity> requestList = requestSessionBeanLocal.retrieveRequestByListingId(singleListing.getListingId());
                System.out.println("*****************Listing ID: " + singleListing.getListingId() + "****************");
                System.out.println("*****************Request list size: " + requestList.size() + "****************");
                for (int j = 0; j < requestList.size(); j++) {
                    requests.add(requestList.get(j));
                }
            }
            RequestMadeRsp reqMadeRsp = new RequestMadeRsp(requests);
            return Response.status(Response.Status.OK).entity(reqMadeRsp).build();
        } catch (CustomerNotFoundException ex) {
            Logger.getLogger(RequestResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Path("acceptRequest/{requestId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response acceptRequest(@PathParam("requestId") Long requestId) throws RequestNotFoundException {
        try {
            RequestEntity request = requestSessionBeanLocal.acceptRequest(requestId);
            CreateRequestRsp result = new CreateRequestRsp(request);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (RequestNotFoundException ex) {
            ErrorRsp errorRsp = new ErrorRsp("Unable to accept request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }
    
    @Path("rejectRequest/{requestId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rejectRequest(@PathParam("requestId") Long requestId) throws RequestNotFoundException {
        try {
            RequestEntity request = requestSessionBeanLocal.rejectRequest(requestId);
            CreateRequestRsp result = new CreateRequestRsp(request);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (RequestNotFoundException ex) {
            ErrorRsp errorRsp = new ErrorRsp("Unable to accept request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }
    
    

    @Path("openedRequest/{requestId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response openedRequest(@PathParam("requestId") Long requestId) {
        try {
            RequestEntity request = requestSessionBeanLocal.openedRequest(requestId);
            System.out.println("**********Successfully executed openedRequest function**********");
            CreateRequestRsp result = new CreateRequestRsp(request);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (RequestNotFoundException ex) {
            ErrorRsp errorRsp = new ErrorRsp("Error while opening request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }
    
    @Path("deleteRequest/{requestId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRequest(@PathParam("requestId") Long requestId){
        try {
            Boolean result = requestSessionBeanLocal.deleteRequestAPI(requestId);
            System.out.println("Reults: " + result);
            DeleteRequestRsp deleteRequestRsp = new DeleteRequestRsp(result);
            return Response.status(Response.Status.OK).entity(deleteRequestRsp).build();
        } catch (RequestNotFoundException ex) {
            ErrorRsp errorRsp = new ErrorRsp("RequestID was not valid");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    private RequestSessionBeanLocal lookupRequestSessionBeanLocal() {
        try {
            javax.naming.Context c = new InitialContext();
            return (RequestSessionBeanLocal) c.lookup("java:global/BorrowMe/BorrowMe-ejb/RequestSessionBean!ejb.session.stateless.RequestSessionBeanLocal");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    private ListingSessionBeanLocal lookupListingSessionBeanLocal() {
        try {
            javax.naming.Context c = new InitialContext();
            return (ListingSessionBeanLocal) c.lookup("java:global/BorrowMe/BorrowMe-ejb/ListingSessionBean!ejb.session.stateless.ListingSessionBeanLocal");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }
}