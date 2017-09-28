/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.security;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sitewhere.microservice.security.SitewhereAuthentication;
import com.sitewhere.microservice.security.SitewhereUserDetails;
import com.sitewhere.microservice.spi.security.ISystemUser;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.user.IGrantedAuthority;
import com.sitewhere.spi.user.IUser;
import com.sitewhere.spi.user.IUserManagement;
import com.sitewhere.web.security.jwt.JwtAuthenticationToken;

/**
 * Spring authentication provider using SiteWhere user management APIs.
 * 
 * @author Derek
 */
public class SiteWhereAuthenticationProvider implements AuthenticationProvider {

    /** Static logger instance */
    @SuppressWarnings("unused")
    private static Logger LOGGER = LogManager.getLogger();

    /** System superuser */
    private ISystemUser systemUser;

    /** User management implementation */
    private IUserManagement userManagement;

    public SiteWhereAuthenticationProvider(ISystemUser systemUser, IUserManagement userManagement) {
	this.systemUser = systemUser;
	this.userManagement = userManagement;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.security.providers.AuthenticationProvider#
     * authenticate(org. springframework.security. Authentication)
     */
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	try {
	    if (authentication instanceof UsernamePasswordAuthenticationToken) {
		return authenticateBasicAuth(authentication);
	    } else if (authentication instanceof JwtAuthenticationToken) {
		return authenticateJwt(authentication);
	    } else if (authentication instanceof SitewhereAuthentication) {
		return authentication;
	    }
	    throw new AuthenticationServiceException("Unknown authentication: " + authentication.getClass().getName());
	} catch (SiteWhereException e) {
	    throw new BadCredentialsException("Unable to authenticate.", e);
	}
    }

    /**
     * Validate basic authentication credentials.
     * 
     * @param authentication
     * @return
     * @throws SiteWhereException
     */
    protected Authentication authenticateBasicAuth(Authentication authentication) throws SiteWhereException {
	String username = (String) authentication.getPrincipal();
	String password = (String) authentication.getCredentials();

	// Swap thread to superuser to authenticate login.
	Authentication previous = SecurityContextHolder.getContext().getAuthentication();
	try {
	    SecurityContextHolder.getContext().setAuthentication(getSystemUser().getAuthentication());
	    IUser user = validateUserManagement().authenticate(username, password, false);
	    List<IGrantedAuthority> auths = getUserManagement().getGrantedAuthorities(user.getUsername());
	    SitewhereUserDetails details = new SitewhereUserDetails(user, auths);
	    return new SitewhereAuthentication(details, password);
	} finally {
	    SecurityContextHolder.getContext().setAuthentication(previous);
	}
    }

    /**
     * Validate basic JWT credentials.
     * 
     * @param authentication
     * @return
     * @throws SiteWhereException
     */
    protected Authentication authenticateJwt(Authentication authentication) throws SiteWhereException {
	// TODO: This is overkill since the auths are in the JWT.
	String username = (String) authentication.getPrincipal();
	String password = (String) authentication.getCredentials();

	// Swap thread to superuser to authenticate login.
	Authentication previous = SecurityContextHolder.getContext().getAuthentication();
	try {
	    SecurityContextHolder.getContext().setAuthentication(getSystemUser().getAuthentication());
	    IUser user = validateUserManagement().getUserByUsername(username);
	    List<IGrantedAuthority> auths = getUserManagement().getGrantedAuthorities(username);
	    SitewhereUserDetails details = new SitewhereUserDetails(user, auths);
	    return new SitewhereAuthentication(details, password);
	} finally {
	    SecurityContextHolder.getContext().setAuthentication(previous);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.security.providers.AuthenticationProvider#supports(
     * java.lang .Class)
     */
    @SuppressWarnings("rawtypes")
    public boolean supports(Class clazz) {
	return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(clazz))
		|| (JwtAuthenticationToken.class.isAssignableFrom(clazz))
		|| (SitewhereAuthentication.class.isAssignableFrom(clazz));
    }

    /**
     * Validate that user management is avaliable.
     * 
     * @return
     * @throws AuthenticationServiceException
     */
    protected IUserManagement validateUserManagement() throws AuthenticationServiceException {
	if (getUserManagement() == null) {
	    throw new AuthenticationServiceException("User management not available. Check logs for details.");
	}
	return getUserManagement();
    }

    public ISystemUser getSystemUser() {
	return systemUser;
    }

    public void setSystemUser(ISystemUser systemUser) {
	this.systemUser = systemUser;
    }

    public IUserManagement getUserManagement() {
	return userManagement;
    }

    public void setUserManagement(IUserManagement userManagement) {
	this.userManagement = userManagement;
    }
}