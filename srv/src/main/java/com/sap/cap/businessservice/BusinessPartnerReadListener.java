package com.sap.cap.businessservice;

import cds.gen.cloud.sdk.capng.CapBusinessPartner;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.odatav2.connectivity.ODataException;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.DefaultBusinessPartnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ServiceName( "cloud.sdk.capng" )
public class BusinessPartnerReadListener implements EventHandler {

    final HttpDestination httpDestination = DestinationAccessor.getDestination("MyErpSystem").asHttp();
    private Map<Object, Map<String, Object>> businessPartner = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(BusinessPartnerReadListener.class);

    @On(event = CdsService.EVENT_READ, entity = "cloud.sdk.capng.CapBusinessPartner")
    public void onRead(CdsReadEventContext context) throws ODataException {


        final Map<Object, Map<String, Object>> result = new HashMap<>();
        httpDestination.getPropertyNames().forEach(e -> logger.warn(httpDestination.get(e,String.class).getOrElse("ss")));
        final List<BusinessPartner> businessPartners =
                new DefaultBusinessPartnerService().getAllBusinessPartner().top(10).execute(httpDestination);


        final List<CapBusinessPartner> capBusinessPartners =
                convertS4BusinessPartnersToCapBusinessPartners(businessPartners, "MyErpSystem");
        capBusinessPartners.forEach(capBusinessPartner -> {
            result.put(capBusinessPartner.getId(), capBusinessPartner);
        });

        context.setResult(result.values());
    }

    @On(event = CdsService.EVENT_CREATE, entity = "cloud.sdk.capng.CapBusinessPartner")
    public void onCreate(CdsCreateEventContext context) throws ODataException {
        //context.getCqn().entries().forEach(e -> businessPartner.put(e.get("CapBusinessPartner"), e));
        BusinessPartnerService service = new DefaultBusinessPartnerService();
        BusinessPartner bp = new BusinessPartner();
        for(Map<String,Object> m : context.getCqn().entries()){
             bp = BusinessPartner.builder().firstName(m.get("firstName").toString()).lastName(m.get("surname").toString()).businessPartner(m.get("ID").toString()).build();
        }
        service.createBusinessPartner(bp).execute(httpDestination);
        //context.setResult(context.getCqn().entries());
    }

    private List<CapBusinessPartner> convertS4BusinessPartnersToCapBusinessPartners(
            final List<BusinessPartner> s4BusinessPartners,
            final String destinationName )
    {
        final List<CapBusinessPartner> capBusinessPartners = new ArrayList<>();

        for( final BusinessPartner s4BusinessPartner : s4BusinessPartners ) {
            final CapBusinessPartner capBusinessPartner = com.sap.cds.Struct.create(CapBusinessPartner.class);

            capBusinessPartner.setFirstName(s4BusinessPartner.getFirstName());
            capBusinessPartner.setSurname(s4BusinessPartner.getLastName());
            capBusinessPartner.setId(s4BusinessPartner.getBusinessPartner());
            capBusinessPartner.setSourceDestination(destinationName);

            capBusinessPartners.add(capBusinessPartner);
        }

        return capBusinessPartners;
    }
}