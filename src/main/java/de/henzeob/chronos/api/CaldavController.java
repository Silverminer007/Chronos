package de.henzeob.chronos.api;

import de.henzeob.chronos.exceptions.CaldavException;
import de.henzeob.chronos.services.CaldavService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/caldav")
public class CaldavController {
    private final CaldavService caldavService;

    public CaldavController(CaldavService caldavService) {
        this.caldavService = caldavService;
    }

    @RequestMapping(value = "/{dateId}.ics")
    public ResponseEntity<String> caldavMethods(@RequestBody String requestBody, @PathVariable("dateId") String dateId, HttpServletRequest httpRequest) {
        try {
            return switch (httpRequest.getMethod()) {
                case "MKCOL", "MKCALENDAR", "REPORT" -> new ResponseEntity<>(HttpStatus.FORBIDDEN);
                case "PROPFIND" -> new ResponseEntity<>(caldavService.findPropertiesForDate(requestBody, dateId), HttpStatus.OK);
                case "PROPPATCH" -> new ResponseEntity<>(caldavService.patchPropertiesForDate(requestBody, dateId), HttpStatus.OK);
                default -> new ResponseEntity<>(HttpStatus.NOT_FOUND);
            };
        } catch (CaldavException e) {
            return new ResponseEntity<>(e.getResponseBody(), HttpStatus.MULTI_STATUS);
        }
    }

    @RequestMapping(value = "/")
    public ResponseEntity<String> caldavCollectionMethods(@RequestBody String requestBody, HttpServletRequest httpRequest) {
        try {
            return switch (httpRequest.getMethod()) {
                case "MKCOL", "MKCALENDAR" -> new ResponseEntity<>(HttpStatus.FORBIDDEN);
                case "PROPFIND" -> new ResponseEntity<>(caldavService.findProperties(requestBody), HttpStatus.OK);
                case "PROPPATCH" -> new ResponseEntity<>(caldavService.patchProperties(requestBody), HttpStatus.OK);
                case "REPORT" -> this.parseReportRequest(requestBody);
                default -> new ResponseEntity<>(HttpStatus.NOT_FOUND);
            };
        } catch (CaldavException e) {
            return new ResponseEntity<>(e.getResponseBody(), HttpStatus.MULTI_STATUS);
        }
    }

    private ResponseEntity<String> parseReportRequest(String requestBody) {
        try {
            ByteArrayInputStream requestBodyAsStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

            Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(requestBodyAsStream);

            String rootElementName = xmlDocument.getDocumentElement().getTagName();

            switch (rootElementName) {
                case "calendar-query" -> {
                    return parseCalendarQueryReport(xmlDocument);
                }
                case "calendar-multiget" -> {
                    return parseCalendarMultiGetReport(xmlDocument);
                }
                case "free-busy-query" -> {
                    return parseFreeBusyQueryReport(xmlDocument);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | CaldavException _) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<String> parseCalendarQueryReport(Document xmlDocument) throws CaldavException {
        List<String> propertyNames = this.getPropertyNames(xmlDocument);

        Node filterNode = xmlDocument.getElementsByTagName("filter").item(0);
        return null;

    }

    private ResponseEntity<String> parseCalendarMultiGetReport(Document xmlDocument) throws CaldavException {
        List<String> propertyNames = this.getPropertyNames(xmlDocument);

        NodeList hrefs = xmlDocument.getElementsByTagName("href");
        List<String> urls = new ArrayList<>();
        for(int i = 0; i < hrefs.getLength(); i++) {
            urls.add(hrefs.item(i).getNodeValue());
        }

        String multiGetResponse = caldavService.calendarMultiGetReport(urls, propertyNames);

        return new ResponseEntity<>(multiGetResponse, HttpStatus.MULTI_STATUS);
    }

    private ResponseEntity<String> parseFreeBusyQueryReport(Document xmlDocument) {
        NodeList timeRangeNodes = xmlDocument.getElementsByTagName("time-range");
        if(timeRangeNodes.getLength() != 1) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        NamedNodeMap timeRangeAttributes = timeRangeNodes.item(0).getAttributes();

        String timeRangeStart = timeRangeAttributes.getNamedItem("start").getNodeValue();
        String timeRangeEnd = timeRangeAttributes.getNamedItem("end").getNodeValue();

        String iCalResponse = caldavService.freeBusyQueryReport(timeRangeStart, timeRangeEnd);

        return new ResponseEntity<>(iCalResponse, HttpStatus.OK);
    }

    private List<String> getPropertyNames(Document xmlDocument) throws CaldavException {
        NodeList propertiesNode = xmlDocument.getElementsByTagName("prop");
        if(propertiesNode.getLength() != 1) {
            throw new CaldavException("");
        }
        NodeList propertiesNodeChildren = propertiesNode.item(0).getChildNodes();
        List<String> propertyNames = new ArrayList<>();
        for(int i = 0; i < propertiesNodeChildren.getLength(); i++) {
            propertyNames.add(propertiesNodeChildren.item(i).getNodeName());
        }
        return propertyNames;
    }
}
