package org.apereo.cas.web.flow;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationResult;
import org.apereo.cas.authentication.AuthenticationResultBuilder;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.DefaultAuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.ticket.InvalidTicketException;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.support.WebUtils;
import org.springframework.web.util.CookieGenerator;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link ServiceWarningAction}. Populates the view
 * with the target url of the application after the warning
 * screen is displayed. 
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class ServiceWarningAction extends AbstractAction {

    private CentralAuthenticationService centralAuthenticationService;

    private AuthenticationSystemSupport authenticationSystemSupport = new DefaultAuthenticationSystemSupport();

    private TicketRegistrySupport ticketRegistrySupport;

    private CookieGenerator warnCookieGenerator;
    
    @Override
    protected Event doExecute(final RequestContext context) throws Exception {
        final HttpServletRequest request = WebUtils.getHttpServletRequest(context);
        final HttpServletResponse response = WebUtils.getHttpServletResponse(context);
        
        final Service service = WebUtils.getService(context);
        final String ticketGrantingTicket = WebUtils.getTicketGrantingTicketId(context);

        final Authentication authentication = this.ticketRegistrySupport.getAuthenticationFrom(ticketGrantingTicket);
        if (authentication == null) {
            throw new InvalidTicketException(
                    new AuthenticationException("No authentication found for ticket " + ticketGrantingTicket), ticketGrantingTicket);
        }
        
        final AuthenticationResultBuilder authenticationResultBuilder = this.authenticationSystemSupport
                .establishAuthenticationContextFromInitial(authentication);
        final AuthenticationResult authenticationResult = authenticationResultBuilder.build(service);

        final ServiceTicket serviceTicketId = this.centralAuthenticationService
                .grantServiceTicket(ticketGrantingTicket, service, authenticationResult);
        WebUtils.putServiceTicketInRequestScope(context, serviceTicketId);
        
        if (request.getParameterMap().containsKey("ignorewarn")) {
            if (Boolean.valueOf(request.getParameter("ignorewarn").toString())) {
                this.warnCookieGenerator.removeCookie(response);
            }
        }
        return new Event(this, CasWebflowConstants.STATE_ID_REDIRECT);
    }

    public void setCentralAuthenticationService(final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    public void setAuthenticationSystemSupport(final AuthenticationSystemSupport authenticationSystemSupport) {
        this.authenticationSystemSupport = authenticationSystemSupport;
    }

    public void setTicketRegistrySupport(final TicketRegistrySupport ticketRegistrySupport) {
        this.ticketRegistrySupport = ticketRegistrySupport;
    }

    public void setWarnCookieGenerator(final CookieGenerator warnCookieGenerator) {
        this.warnCookieGenerator = warnCookieGenerator;
    }
}
