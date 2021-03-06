package org.skyve.impl.web.faces.pipeline.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.primefaces.component.message.Message;
import org.skyve.impl.metadata.Container;
import org.skyve.impl.metadata.view.AbsoluteWidth;
import org.skyve.impl.metadata.view.HorizontalAlignment;
import org.skyve.impl.metadata.view.LayoutUtil;
import org.skyve.impl.metadata.view.RelativeSize;
import org.skyve.impl.metadata.view.container.HBox;
import org.skyve.impl.metadata.view.container.VBox;
import org.skyve.impl.metadata.view.container.form.Form;
import org.skyve.impl.metadata.view.container.form.FormColumn;
import org.skyve.impl.metadata.view.container.form.FormItem;
import org.skyve.impl.metadata.view.container.form.FormRow;
import org.skyve.impl.web.faces.FacesUtil;
import org.skyve.impl.web.faces.pipeline.ResponsiveFormGrid;
import org.skyve.impl.web.faces.pipeline.ResponsiveFormGrid.ResponsiveGridStyle;
import org.skyve.metadata.MetaData;

public class ResponsiveLayoutBuilder extends TabularLayoutBuilder {
/*
	@Override
	public UIComponent toolbarLayout() {
		return panelGroup(false, false, true, null);
//		return responsiveColumn(null, null, false);
	}
*/	

	/**
	 * There's only 1 toolbar for this layout and its at the top.
	 */
	@Override
	public void addToolbarsOrLayouts(UIComponent view, List<UIComponent> toolbarsOrLayouts) {
		HtmlPanelGroup div = panelGroup(false, false, true, null, null);
		div.setStyleClass("ui-g-12");
		div.getChildren().add(toolbarsOrLayouts.get(0));
		view.getChildren().add(0, div);
	}

	@Override
	public UIComponent viewLayout() {
		return responsiveColumn(null, Integer.valueOf(12), null, null, true);
	}
	
	@Override
	public UIComponent tabLayout() {
		return responsiveContainer(null, null);
	}
	
	@Override
	public UIComponent vboxLayout(VBox vbox) {
		return responsiveContainer(vbox.getInvisibleConditionName(), vbox.getWidgetId());
	}
	
	@Override
	public UIComponent hboxLayout(HBox hbox) {
		return responsiveContainer(hbox.getInvisibleConditionName(), hbox.getWidgetId());
	}

	@Override
	public UIComponent addToContainer(Container viewContainer, 
										UIComponent container, 
										UIComponent componentToAdd,
										Integer pixelWidth, 
										Integer responsiveWidth,
										Integer percentageWidth,
										String widgetInvisible) {
		Integer mutablePercentageWidth = percentageWidth;
		boolean nopad = false;

		// If we have layout within Layout lovin', use nopad to keep the lineup real!
		if ((container instanceof HtmlPanelGroup) && (componentToAdd instanceof HtmlPanelGroup)) {
			nopad = true;
		}
/*
		// NB View is an implicit VBox
		if ((viewContainer instanceof VBox) || (viewContainer instanceof org.skyve.metadata.view.View)) {
			// we are adding more responsive layout, go with nopad
			if (componentToAdd instanceof HtmlPanelGroup) {
				nopad = true;
			}
		}
*/
		if ((pixelWidth == null) && 
				(responsiveWidth == null) && 
				(percentageWidth == null) && 
				(viewContainer instanceof HBox)) {
			int unsizedCols = 0;
			int mediumColsRemaining = LayoutUtil.MAX_RESPONSIVE_WIDTH_COLUMNS;
			for (MetaData contained : viewContainer.getContained()) {
				if (contained instanceof AbsoluteWidth) {
					Integer containedPixelWidth = ((AbsoluteWidth) contained).getPixelWidth();
					if (containedPixelWidth != null) {
						mediumColsRemaining -= LayoutUtil.pixelWidthToMediumResponsiveWidth(containedPixelWidth.doubleValue());
					}
					else if (contained instanceof RelativeSize) {
						Integer containedPercentageWidth = ((RelativeSize) contained).getPercentageWidth();
						if (containedPercentageWidth != null) {
							mediumColsRemaining -= LayoutUtil.percentageWidthToResponsiveWidth(containedPercentageWidth.doubleValue());
						}
						else {
							unsizedCols++;
						}
					}
					else {
						unsizedCols++;
					}
				}
				else {
					unsizedCols++;
				}
			}
			mutablePercentageWidth = Integer.valueOf(LayoutUtil.responsiveWidthToPercentageWidth(mediumColsRemaining / unsizedCols));
		}
		HtmlPanelGroup div = responsiveColumn(pixelWidth, responsiveWidth, mutablePercentageWidth, widgetInvisible, nopad);
		div.getChildren().add(componentToAdd);
		container.getChildren().add(div);
		return componentToAdd;
	}
	
	@Override
	public UIComponent addedToContainer(Container viewContainer, UIComponent container) {
		return container.getParent().getParent(); // account for the previously pushed component, and the grid css div
	}
	
	@Override
	public UIComponent formLayout(Form form) {
		// Add the set of form column styles to the Faces ViewRoot.
		ResponsiveGridStyle[] formColumnStyles = responsiveFormStyleClasses(form.getColumns());
		ResponsiveFormGrid grid = new ResponsiveFormGrid(formColumnStyles);
		addResponsiveStyles(grid);
		
		HtmlPanelGroup result = panelGroup(false, false, true, form.getInvisibleConditionName(), form.getWidgetId());
		result.setStyleClass("ui-g ui-g-nopad ui-fluid");
		return result;
	}

	// Used to create dynamic style classes for responsive form layouts
	private int formIndex = Integer.MIN_VALUE;
	
	/**
	 * Add the responsive form grid to the faces view root.
	 * Return the index to the responsive form grid added for use
	 * in the EL expressions in the ensuing form markup.
	 * @param grid	The responsive form layout grid definition
	 */
	public void addResponsiveStyles(ResponsiveFormGrid grid) {
		Map<String, Object> attributes = FacesContext.getCurrentInstance().getViewRoot().getAttributes();
		@SuppressWarnings("unchecked")
		List<ResponsiveFormGrid> formStyles = (List<ResponsiveFormGrid>) attributes.get(FacesUtil.FORM_STYLES_KEY);
		if (formStyles == null) {
			formStyles = new ArrayList<>(5);
			attributes.put(FacesUtil.FORM_STYLES_KEY, formStyles);
		}
		formStyles.add(grid);
		formIndex = formStyles.size() - 1;
	}

	@Override
	public UIComponent formRowLayout(FormRow row) {
		HtmlPanelGroup result = panelGroup(false, false, true, null, null);
		// style="<repsonsive column reset method call>"
		String expression = String.format("#{%s.resetResponsiveFormStyle(%s)}", 
											managedBeanName, 
											Integer.toString(formIndex));
		result.setValueExpression("styleClass", 
									ef.createValueExpression(elc, expression, String.class));
		return result;
	}
	
	@Override
	public UIComponent addedFormRowLayout(UIComponent rowLayout) {
		return rowLayout.getParent();
	}

	// respect responsive width if it is defined in this renderer
	@Override
	protected void setSize(UIComponent component, 
							String existingStyle, 
							Integer pixelWidth, 
							Integer responsiveWidth,
							Integer percentageWidth, 
							Integer pixelHeight, 
							Integer percentageHeight, 
							Integer defaultPercentageWidth) {
		if (responsiveWidth != null) {
			super.setSize(component, existingStyle, null, responsiveWidth, null, pixelHeight, percentageHeight, null);
		}
		else {
			super.setSize(component, existingStyle, pixelWidth, responsiveWidth, percentageWidth, pixelHeight, percentageHeight, null);
		}
	}
	
	@Override
	public void layoutFormItem(UIComponent formOrRowLayout, 
								UIComponent formItemComponent, 
								Form currentForm,
								FormItem currentFormItem, 
								int currentFormColumn,
								String widgetLabel, 
								boolean widgetRequired,
								String widgetInvisible,
								boolean widgetShowsLabelByDefault,
								String widgetHelpText) {
		// The label
		boolean showLabel = widgetShowsLabelByDefault;
		Boolean itemShowLabel = currentFormItem.getShowLabel();
		if (itemShowLabel != null) {
			showLabel = itemShowLabel.booleanValue();
		}
		if (showLabel) {
			String label = currentFormItem.getLabel();
			if (label == null) {
				label = widgetLabel;
			}
			if (label != null) {
				HtmlPanelGroup div = panelGroup(false, false, true, null, null);
				setInvisible(div, widgetInvisible, null);
				// style="<repsonsive column calc method call>"
                String alignment = alignment(currentFormItem.getLabelHorizontalAlignment(), true);
				String expression = String.format("#{%s.getResponsiveFormStyle(%s, null, 1)} %s", 
													managedBeanName,
													Integer.toString(formIndex),
													alignment);
				div.setValueExpression("styleClass", 
										ef.createValueExpression(elc, expression, String.class));
				formOrRowLayout.getChildren().add(div);
				HtmlOutputLabel l = label(label, formItemComponent.getId(), widgetRequired);
				div.getChildren().add(l);
			}
		}
		
		// The field
		Integer colspan = currentFormItem.getColspan();
		HtmlPanelGroup div = panelGroup(false, false, true, null, null);
		setInvisible(div, widgetInvisible, null);
		
		// Create a grid
		String helpText = (Boolean.FALSE.equals(currentFormItem.getShowHelp()) ? null : widgetHelpText);
		HtmlPanelGrid pg = (HtmlPanelGrid) a.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
		setId(pg, null);
		pg.setCellpadding("0"); //Don't pad cells
		pg.setStyleClass("inputComponent");
		pg.setColumns((helpText != null) ? 3 : 2);
		// First (and possibly 3rd if there is help defined) column(s) should shrink
		pg.setColumnClasses((helpText != null) ? "shrink,,shrink" : "shrink");
		div.getChildren().add(pg);
		Message m = message(formItemComponent.getId());
		m.setStyleClass("formMessageStyle");
		pg.getChildren().add(m);
		pg.getChildren().add(formItemComponent);
		if (helpText != null) {
			HtmlOutputText output = new HtmlOutputText();
			output.setEscape(false);
			output.setValue(String.format("<i class=\"fa fa-info-circle help\" data-tooltip=\"%s\"></i>",
											helpText));
			pg.getChildren().add(output);
		}
		
		// Update div's style
		// colspan should be 1.
		if ((colspan == null) || (colspan.intValue() <= 1)) {
			// style="<repsonsive column calc method call>"
			String expression = String.format("#{%s.getResponsiveFormStyle(%s, '%s', 1)}", 
												managedBeanName, 
												Integer.toString(formIndex),
												alignment(currentFormItem.getHorizontalAlignment(), false));
			div.setValueExpression("styleClass", 
									ef.createValueExpression(elc, expression, String.class));
		}
		else { // colspan > 1
			// style="<repsonsive column calc method call>"
			String expression = String.format("#{%s.getResponsiveFormStyle(%s, '%s', %d)}", 
												managedBeanName, 
												Integer.toString(formIndex),
												alignment(currentFormItem.getHorizontalAlignment(), false),
												colspan);
			div.setValueExpression("styleClass", 
									ef.createValueExpression(elc, expression, String.class));
		}
		formOrRowLayout.getChildren().add(div);
	}
	
	private static String alignment(HorizontalAlignment alignment, boolean forFormLabel) {
		String result = null;
		if (alignment == null) {
			result = forFormLabel ? 
						HorizontalAlignment.right.toAlignmentString() : 
						HorizontalAlignment.left.toAlignmentString();
		}
		else {
			result = alignment.toAlignmentString();
		}

		return (forFormLabel ? (result + "FormLabel") : result);
	}
	
	private HtmlPanelGroup responsiveContainer(String invisibleConditionName, String widgetId) {
		HtmlPanelGroup result = panelGroup(false, false, true, null, widgetId);
		setInvisible(result, invisibleConditionName, null);
		result.setStyleClass("ui-g");
		return result;
	}

	private HtmlPanelGroup responsiveColumn(Integer pixelWidth, 
												Integer responsiveWidth, 
												Integer percentageWidth, 
												String widgetInvisible,
												boolean nopad) {
		HtmlPanelGroup result = panelGroup(false, false, true, null, null);
		
		String responsiveGridStyleClasses = responsiveGridStyleClasses(pixelWidth, responsiveWidth, percentageWidth);
		if (responsiveGridStyleClasses != null) {
			result.setStyleClass(nopad ? responsiveGridStyleClasses + " ui-g-nopad" : responsiveGridStyleClasses);
		}
		setInvisible(result, widgetInvisible, null);
		return result;
	}

	private static String responsiveGridStyleClasses(Integer pixelWidth, Integer responsiveWidth, Integer percentageWidth) {
		if (responsiveWidth != null) {
			int width = responsiveWidth.intValue();
			return new ResponsiveGridStyle(width, width).toString();
		}
		else if (pixelWidth != null) {
			double width = pixelWidth.doubleValue();
			int medium = LayoutUtil.pixelWidthToMediumResponsiveWidth(width);
			int large = LayoutUtil.pixelWidthToLargeResponsiveWidth(width);
			return new ResponsiveGridStyle(medium, large).toString();
		}
		else if (percentageWidth != null) {
			int width = LayoutUtil.percentageWidthToResponsiveWidth(percentageWidth.doubleValue());
			return new ResponsiveGridStyle(width, width).toString();
		}
		
		return "ui-g-12";
	}
	
	private static ResponsiveGridStyle[] responsiveFormStyleClasses(List<FormColumn> formColumns) {
		ResponsiveGridStyle[] result = new ResponsiveGridStyle[formColumns.size()];
		
		// max number of columns
		int mediumColsRemaining = LayoutUtil.MAX_RESPONSIVE_WIDTH_COLUMNS;
		int largeColsRemaining = LayoutUtil.MAX_RESPONSIVE_WIDTH_COLUMNS;
		
		int unsizedCols = 0;
		
		for (int i = 0, l = formColumns.size(); i < l; i++) {
			FormColumn formColumn = formColumns.get(i);
			Integer pixelWidth = formColumn.getPixelWidth();
			Integer responsiveWidth = formColumn.getResponsiveWidth();
			Integer percentageWidth = formColumn.getPercentageWidth();
			if (responsiveWidth != null) {
				int width = responsiveWidth.intValue();
				mediumColsRemaining -= width;
				largeColsRemaining -= width;
				result[i] = new ResponsiveGridStyle(width, width);
			}
			else if (pixelWidth != null) {
				double width = pixelWidth.doubleValue();
				int medium = LayoutUtil.pixelWidthToMediumResponsiveWidth(width);
				int large = LayoutUtil.pixelWidthToLargeResponsiveWidth(width);
				mediumColsRemaining -= medium;
				largeColsRemaining -= large;
				result[i] = new ResponsiveGridStyle(medium, large);
			}
			else if (percentageWidth != null) {
				int cols = LayoutUtil.percentageWidthToResponsiveWidth(percentageWidth.doubleValue());
				mediumColsRemaining -= cols;
				largeColsRemaining -= cols;
				result[i] = new ResponsiveGridStyle(cols, cols);
			}
			else {
				unsizedCols++;
			}
		}

		if (unsizedCols > 0) {
			int medium = mediumColsRemaining / unsizedCols;
			int large = largeColsRemaining / unsizedCols;

			for (int i = 0, l = formColumns.size(); i < l; i++) {
				if (result[i] == null) {
					result[i] = new ResponsiveGridStyle(medium, large);
				}
			}
		}
		
		return result;
	}
}
