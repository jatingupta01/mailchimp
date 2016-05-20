package com.ttnd.mailchimp.model;

import java.security.Principal;


public class SimplePrincipal implements Principal {
	protected final String name;

    public SimplePrincipal(String name) {
        if (name.length() == 0) {
            throw new IllegalArgumentException("Principal name cannot be blank.");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Principal) {
            return name.equals(((Principal) obj).getName());
        }
        return false;
    }
}
