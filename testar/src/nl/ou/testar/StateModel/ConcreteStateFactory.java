package nl.ou.testar.StateModel;

import org.fruit.alayer.State;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;
import java.util.Set;

public abstract class ConcreteStateFactory {

    /**
     * This builder method will create a new concrete state class and populate it with the needed data
     * @param newState the testar State to serve as a base
     * @param tags the tags containing the atributes that were used in the construction of the concrete state id
     * @return the new concrete state
     */
    public static ConcreteState createConcreteState(State newState, Set<Tag<?>> tags) {
        String concreteStateId = newState.get(Tags.ConcreteIDCustom);
        ConcreteState concreteState = new ConcreteState(concreteStateId, tags);

        // next we want to add all the attributes contained in the state, and then do the same thing for the child widgets
        setAttributes(concreteState, newState);
        copyWidgetTreeStructure(newState, concreteState, concreteState);

        return concreteState;
    }

    /**
     * Helper method to transfer attribute information from the testar enitities to our own entities.
     * @param widget
     * @param testarWidget
     */
    private static void setAttributes(Widget widget, org.fruit.alayer.Widget testarWidget) {
        for (Tag<?> t : testarWidget.tags()) {
            widget.addAttribute(t, testarWidget.get(t, null));
        }
    }

    /**
     * This method recursively populates the widget tree in our own models.
     * @param testarWidget
     * @param stateModelWidget
     * @param rootWidget
     */
    private static void copyWidgetTreeStructure(org.fruit.alayer.Widget testarWidget, Widget stateModelWidget, ConcreteState rootWidget) {
        // we loop through the testar widget's children to copy their attributes into new widgets
        for (int i = 0; i < testarWidget.childCount(); i++) {
            org.fruit.alayer.Widget testarChildWidget = testarWidget.child(i);
            String widgetId = testarChildWidget.get(Tags.ConcreteIDCustom);
            Widget newStateModelWidget = new Widget(widgetId);
            newStateModelWidget.setRootWidget(rootWidget);
            // copy the attribute info
            setAttributes(newStateModelWidget, testarChildWidget);
            // then add the new model widget to the tree
            stateModelWidget.addChild(newStateModelWidget);
            // recursively deal with the entire widget tree
            copyWidgetTreeStructure(testarChildWidget, newStateModelWidget, rootWidget);
        }
    }

}
