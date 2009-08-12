package com.sunlightlabs.entities;

/**
 * Object representing an immutable name-value pair
 * com.sunlightlabs.entities.NameValuePair
 * steve Jul 22, 2009
 */
public class NameValuePair {
	public static Class<NameValuePair> THIS_CLASS = NameValuePair.class;
	public static NameValuePair[] EMPTY_ARRAY = {};
	
	private final String m_Name;
	private final String m_Value;
	public NameValuePair(String name, String value) {
		super();
		m_Name = name;
		m_Value = value;
	}
	/**
	 * probably non-null name
	 * @return probably non-null name
	 */
	public String getName() {
		return m_Name;
	}
	/**
	 * probably non-null value
	 * @return probably non-null value
	 */
	public String getValue() {
		return m_Value;
	}
	
	/**
	 * so this can be used as a hash key
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_Name == null) ? 0 : m_Name.hashCode());
		result = prime * result + ((m_Value == null) ? 0 : m_Value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NameValuePair other = (NameValuePair) obj;
		if (m_Name == null) {
			if (other.m_Name != null)
				return false;
		} else if (!m_Name.equals(other.m_Name))
			return false;
		if (m_Value == null) {
			if (other.m_Value != null)
				return false;
		} else if (!m_Value.equals(other.m_Value))
			return false;
		return true;
	}
	
	/**
	 * so this can be used as a hash key
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getName() + "=" + getValue();
	}
	
	
	
}
