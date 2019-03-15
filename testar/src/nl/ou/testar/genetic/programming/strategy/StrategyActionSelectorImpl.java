package nl.ou.testar.genetic.programming.strategy;

import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNode;
import nl.ou.testar.genetic.programming.strategy.actionTypes.StrategyNodeAction;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;

import java.util.Set;

public class StrategyActionSelectorImpl implements StrategyActionSelector {

    private StrategyNodeAction strategyTree;
    private StrategyGuiStateImpl stateManager;

    StrategyActionSelectorImpl(final StrategyNode strategy) {
        System.out.println("DEBUG: creating genetic programming strategy");
        if (strategy instanceof StrategyNodeAction) {
            strategyTree = (StrategyNodeAction) strategy;
        } else {
            throw new RuntimeException("strategy is not of type 'StrategyNodeAction'!");
        }
        stateManager = new StrategyGuiStateImpl();
    }

    @Override
    public void print() {
        strategyTree.print(0);
    }

    @Override
    public Action selectAction(final State state, final Set<Action> actions) {
        stateManager.updateState(state, actions);
        final Action action = strategyTree.getAction(stateManager)
                .orElse(stateManager.getRandomAction());
        this.updateState(action, state);
        System.out.printf("The selected action is of type %s \n", action.get(Tags.Role));

        return action;
    }

    @Override
    public void printMetrics() {
        System.out.printf("Total number of actions %d \n", stateManager.getTotalNumberOfActions());
        System.out.printf("Total number of unique actions %d \n", stateManager.getTotalNumberOfUniqueExecutedActions());
        stateManager.printActionWithTimeExecuted();
        System.out.printf("Total number of states visited %d \n", stateManager.getTotalVisitedStates());
        System.out.printf("Total number of unique states %d \n", stateManager.getTotalNumberOfUniqueStates());
    }

    @Override
    public Metric getMetrics() {
        return new Metric(
                stateManager.getTotalVisitedStates(),
                stateManager.getTotalNumberOfActions(),
                stateManager.getTotalNumberOfUniqueStates(),
                stateManager.getTotalNumberOfUniqueExecutedActions()
        );
    }

    @Override
    public void setTags(final Tag<String> stateTag) {
        stateManager.setStateTag(stateTag);
    }

    @Override
    public void clear() {
        stateManager.clear();
    }

    private void updateState(final Action action, final State state) {
        stateManager.addActionToPreviousActions(action);
        stateManager.addStateToPreviousStates(state);
    }
}
