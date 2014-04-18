package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.List;

public class Nomination implements Serializable {
    private static final long serialVersionUID = 2L;

	public String nomination_id, number, organization;

    public List<Nominee> nominees;

	public static class Nominee implements Serializable {
        private static final long serialVersionUID = 3L;
		public String name, position, state;
	}

    // assumes nomination and nominees are non-null
    public static String nomineesFor(Nomination nomination) {
        StringBuffer fullDesc = new StringBuffer();
        int count = nomination.nominees.size();
        for (int i=0; i<count; i++) {
            Nomination.Nominee nominee = nomination.nominees.get(i);

            StringBuffer desc = new StringBuffer();
            if (nominee.name != null)
                desc.append(nominee.name);
            if (nominee.state != null)
                desc.append(" (" + nominee.state + ")");
            if (nominee.position != null)
                desc.append(" for " + nominee.position);

            fullDesc.append(desc);
            if (i < count-1)
                fullDesc.append(", ");
        }

        return fullDesc.toString();
    }
}