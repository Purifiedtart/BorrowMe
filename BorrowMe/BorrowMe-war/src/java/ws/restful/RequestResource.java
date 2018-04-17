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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import ws.restful.datamodel.Request.CreateRequestReq;
import ws.restful.datamodel.Request.CreateRequestRsp;
import ws.restful.datamodel.Listing.ErrorRsp;
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
                System.err.println("********** Date: " + createRequestReq.getStartDateStr());
                System.err.println("********** Date: " + createRequestReq.getEndDateStr());
                Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(createRequestReq.getStartDateStr());
                Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(createRequestReq.getEndDateStr());
                RequestEntity rq = requestSessionBeanLocal.createRequestAPI(createRequestReq.getRequest(), requesterId, listingId, startDate, endDate);
                System.out.println("******************************************************");
                CreateRequestRsp rsp = new CreateRequestRsp(rq);
                return Response.status(Response.Status.OK).entity(rsp).build();
            } catch (ParseException | CreateRequestException ex) {
                ex.printStackTrace();
            }

        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid create request request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
        return null;
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
    public Response requestReceived(@PathParam("customerId") Long customerId) throws CustomerNotFoundException {
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
            throw new CustomerNotFoundException("Customer not found exception: " + ex.getMessage());
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
