package org.skyve.impl.web.faces.converters.time;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.skyve.domain.types.TimeOnly;
import org.skyve.impl.util.UtilImpl;

public class HH24_MI extends org.skyve.domain.types.converters.time.HH24_MI implements Converter {
	@Override
	public Object getAsObject(FacesContext fc, UIComponent component, String value) {
    	String processedValue = UtilImpl.processStringValue(value);
    	if (processedValue != null) {
			try {
				return fromDisplayValue(processedValue);
			}
			catch (Exception e) {
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
																"Invalid time (use HH24:MI format)",
																"Invalid time (use HH24:MI format)"),
												e);
			}
    	}
    	return null;
	}

	@Override
	public String getAsString(FacesContext fc, UIComponent component, Object value) {
		try {
			return toDisplayValue((TimeOnly) value);
		}
		catch (Exception e) {
			return null;
		}
	}
}
