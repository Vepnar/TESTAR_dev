/***************************************************************************************************
*
* Copyright (c) 2015, 2016, 2017 Universitat Politecnica de Valencia - www.upv.es
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
* notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from
* this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************************************/


package es.upv.staq.testar.algorithms;

import java.util.Set;

import org.fruit.alayer.Action;
import org.fruit.alayer.State;

import es.upv.staq.testar.graph.IEnvironment;
import es.upv.staq.testar.graph.IGraphState;
import es.upv.staq.testar.graph.WalkStopper;
import es.upv.staq.testar.prolog.JIPrologWrapper;

/**
 * 
 * @author Urko Rueda Molina (alias: urueda)
 *
 */
public interface IWalker {
	
	/**
	 * Sets prolog service.
	 * @param jipWrapper A wrapper to prolog.
	 */
	public void setProlog(JIPrologWrapper jipWrapper);
	
	/**
	 * The base reward to use.
	 * @return The base reward.
	 */
	public double getBaseReward();	
	
	/**
	 * Enables and disables walking a previous test.
	 * How-to:
	 *   1) enablePreviousWalk()
	 *   2) populate previous graph contents
	 *   3) disablePreviousWalk()
	 */
	public void enablePreviousWalk();
	public void disablePreviousWalk();

	/**
	 * Walking algorithm.
	 * @param env Graph environment.
	 * @param walkStopper A walk stopping criteria.
	 */
	public void walk(IEnvironment env, WalkStopper walkStopper);
	
	/**
	 * Selects an action to be executed from a set of available actions for a SUT state.
	 * @param env Graph environment.
	 * @param state SUT state.
	 * @param actions Available actions for SUT state.
	 * @return The selected algorithm action.
	 */
	public Action selectAction(IEnvironment env, State state, Set<Action> actions, JIPrologWrapper jipWrapper);	

	/**
	 * Gets a reward for a state taking into account its derived UI actions and the target states for each action.
	 * @param env Graph environment.
	 * @param state Graph state.
	 * @return A rewarding score for the state between 0.0 (worst) and >0.0 (best).
	 */
	public double getStateReward(IEnvironment env, IGraphState state);
	/**
	 * Proportional action selection.
	 * @param env Graph environment.
	 * @param state A state.
	 * @param actions State' actions.
	 * @return A proportional selected action.
	 */
	public Action selectProportional(IEnvironment env, State state, Set<Action> actions);

}
